package cn.zflzqy.mysqldatatoes.config;

import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.thread.ThreadPoolFactory;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(MysqlDataToEsPropertites.class)
public class MysqlDataToEsConfig {

    private static final Logger log = LoggerFactory.getLogger(MysqlDataToEsConfig.class);

    private static DebeziumEngine<ChangeEvent<String, String>> engine;

    private ThreadPoolExecutor poolExecutor;

    @Autowired
    private MysqlDataToEsPropertites mysqlDataToEsPropertites;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @PostConstruct
    public void start() throws Exception {
        // 定义mysql连接
        final Properties props = new Properties();
        props.setProperty("name", "engine");
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        // redis记录偏移量
        props.setProperty("offset.storage", "io.debezium.storage.redis.offset.RedisOffsetBackingStore");
        props.setProperty("offset.storage.redis.address", mysqlDataToEsPropertites.getRedisUrl());
        props.setProperty("offset.storage.redis.password", mysqlDataToEsPropertites.getRedisPassword());
        props.setProperty("offset.flush.interval.ms", "60000");

        /* 设置属性信息*/
        JdbcUrlParser.JdbcConnectionInfo jdbcConnectionInfo = JdbcUrlParser.parseJdbcUrl(mysqlDataToEsPropertites.getMysqlUrl());
        props.setProperty("database.hostname", jdbcConnectionInfo.getHost());
        props.setProperty("database.port", String.valueOf(jdbcConnectionInfo.getPort()));
        props.setProperty("database.user", mysqlDataToEsPropertites.getMysqlUsername());
        props.setProperty("database.password", mysqlDataToEsPropertites.getMysqlPassword());
        props.setProperty("schema.history.internal", "io.debezium.storage.redis.history.RedisSchemaHistory");
        props.setProperty("schema.history.internal.redis.address", mysqlDataToEsPropertites.getRedisUrl());
        props.setProperty("schema.history.internal.redis.password", mysqlDataToEsPropertites.getRedisPassword());
        props.setProperty("topic.prefix", "my-app-connector");
        // 设置默认即可，但是会存在多项目的情况下serverid偏移的问题 todo
        props.setProperty("database.server.id", "185744");

        props.setProperty("database.include.list", jdbcConnectionInfo.getDatabase());
        // 要捕获的数据表
        props.setProperty("table.include.list", jdbcConnectionInfo.getDatabase() + ".*");
        props.setProperty("time.precision.mode", "connect");

        props.setProperty("database.serverTimezone", "Asia/Shanghai");
        props.setProperty("database.connectionTimeZone", "Asia/Shanghai");
        props.setProperty("database.server.name", "my-app-connector");

        // 扫描实体
        PackageScan.scanEntities(mysqlDataToEsPropertites.getBasePackage());
        Map<String, Class> indexs = PackageScan.getIndexs();

        // 数据处理执行类
        Execute execute = new Execute();

        // 创建engine
        try {
            engine = DebeziumEngine.create(Json.class)
                    .using(props)
                    .notifying(record -> {
                        try {
                            // 处理数据，推送到es
                            String value = record.value();
                            if (!StringUtils.hasText(value)) {
                                log.warn("没有数据value的值");
                                return;
                            }
                            // 创建 Gson 对象
                            Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

                            // 将字符串转换为 JsonObject
                            JsonObject jsonObject = gson.fromJson(value, JsonObject.class);
                            JsonObject payload = jsonObject.getAsJsonObject("payload");
                            JsonObject source = payload.getAsJsonObject("source");
                            String table = source.get("table").getAsString();
                            if (!indexs.containsKey(table)) {
                                log.warn("未配置{}的索引实体，跳过", table);
                                return;
                            }

                            // 检查是否存在索引
                            boolean exists = elasticsearchRestTemplate.indexOps(indexs.get(table)).exists();
                            if (!exists) {
                                elasticsearchRestTemplate.indexOps(indexs.get(table)).create();
                            }
                            // 处理数据
                            execute.execute(jsonObject);
                            OpEnum opEnum = OpEnum.valueOf(payload.get("op").getAsString());
                            // 根据不同的crud类型返回不同的数据
                            switch (opEnum) {
                                case r:
                                    elasticsearchRestTemplate.delete(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                    elasticsearchRestTemplate.save(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                    break;
                                case c:
                                    elasticsearchRestTemplate.save(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                    break;
                                case u:
                                    elasticsearchRestTemplate.delete(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                    elasticsearchRestTemplate.save(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                    break;
                                case d:
                                    elasticsearchRestTemplate.delete(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
                                default:
                            }


                        } catch (Exception e) {
                            log.error("处理异常", e);
                        }
                    }).build();

            poolExecutor = ThreadPoolFactory.build();
            poolExecutor.execute(engine);
        } catch (Exception e) {
            log.error("创建 engine 失败", e);
        }
    }

    @PreDestroy
    private void destory() throws Exception {
        if (engine != null) {
            engine.close();
        }
        if (poolExecutor != null) {
            poolExecutor.shutdown();
        }
    }

}