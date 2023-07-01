package cn.zflzqy.mysqldatatoes.config;

import cn.zflzqy.mysqldatatoes.annotation.RequestUrl;
import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import cn.zflzqy.mysqldatatoes.execute.SyncDatatExcute;
import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import cn.zflzqy.mysqldatatoes.handler.TransDateHandler;
import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.thread.ThreadPoolFactory;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MysqlDataToEsPropertites.class)
public class MysqlDataToEsConfig {


    private static final Logger log = LoggerFactory.getLogger(MysqlDataToEsConfig.class);

    private static DebeziumEngine<ChangeEvent<String, String>> engine;

    private ThreadPoolExecutor poolExecutor;

    @Autowired
    private MysqlDataToEsPropertites mysqlDataToEsPropertites;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Bean
    public SyncDatatExcute syncDatatExcute(){
        return new SyncDatatExcute();
    }


    @PostConstruct
    public void start() {

        // 扫描实体
        PackageScan.scanEntities(StringUtils.hasText(mysqlDataToEsPropertites.getBasePackage())?
                mysqlDataToEsPropertites.getBasePackage():
                PackagePathResolver.mainClassPackagePath);
        Map<String, Class> indexs = PackageScan.getIndexs();

        // 构建执行参数
        Properties props = buildPropertites(indexs);


        // 数据处理执行类
        Execute execute = new Execute(HandlerEnum.INCREMENTAL);
        HandlerService.register(execute,new TransDateHandler());
        // 创建engine
        try {
            engine = DebeziumEngine.create(Json.class)
                    .using(props)
                    .notifying(new CustomConsumer(indexs,execute))
                    .build();

            poolExecutor = ThreadPoolFactory.build();
            poolExecutor.execute(engine);
        } catch (Exception e) {
            log.error("创建 engine 失败", e);
        }
    }

    /**
     * @description 数据消费
     */
    class CustomConsumer implements Consumer<ChangeEvent<String, String>> {
        private  Map<String, Class> indexs;
        private  Execute execute;

        public CustomConsumer(Map<String, Class> indexs, Execute execute) {
            this.indexs = indexs;
            this.execute = execute;
        }

        @Override
        public void accept(ChangeEvent<String, String> record) {
            if (record==null||!StringUtils.hasText(record.value())){
                log.warn("没有数据value的值");
                return;
            }

            // 处理的数据
            String value = record.value();
            try {
                // 将字符串转换为 JsonObject
                JSONObject jsonObject = JSONObject.parseObject(value);
                JSONObject payload = jsonObject.getJSONObject("payload");
                JSONObject source = payload.getJSONObject("source");
                String table = source.getString("table");
                if (!indexs.containsKey(table)) {
                    log.info("未配置{}的索引实体，跳过", table);
                    return;
                }

                // 检查是否存在索引
                boolean exists = elasticsearchRestTemplate.indexOps(indexs.get(table)).exists();
                if (!exists) {
                    elasticsearchRestTemplate.indexOps(indexs.get(table)).createWithMapping();
                }

                // 处理数据
                execute.execute(jsonObject,indexs.get(table));

                // 获取操作类型
                OpEnum opEnum = null;
                try {
                    opEnum = OpEnum.valueOf(payload.getString("op"));
                }catch (Exception e) {
                    log.warn("无相关枚举:{}",payload.getString("op"));
                    return;
                }
                switch (opEnum) {
                    case r:
                    case c:
                    case u:
                        addEsData(elasticsearchRestTemplate,indexs, payload.getJSONObject("after"), table, opEnum);
                        break;
                    case d:
                        addEsData(elasticsearchRestTemplate,indexs, payload.getJSONObject("before"), table, opEnum);
                    default:
                }
            } catch (Exception e) {
                log.error("处理异常", e);
            }
        }
    }


    @PreDestroy
    private void destroy() throws Exception {
        if (engine != null) {
            engine.close();
        }
        if (poolExecutor != null) {
            poolExecutor.shutdown();
        }
    }


    /**
     * @description 添加数据到es
     * @param elasticsearchRestTemplate
     * @param indexs
     * @param data
     * @param table
     * @param opEnum
     */
    public static void addEsData(ElasticsearchRestTemplate elasticsearchRestTemplate,
                                 Map<String, Class> indexs, JSONObject data, String table, OpEnum opEnum) {
        List<JSONObject> datas = new ArrayList<JSONObject>();
        datas.add(data);
        addEsData(elasticsearchRestTemplate, indexs,datas,table,opEnum);

    }

    /**
     * @description 添加数据到es
     * @param elasticsearchRestTemplate
     * @param indexs
     * @param data
     * @param table
     * @param opEnum
     */
    public static void addEsData(ElasticsearchRestTemplate elasticsearchRestTemplate,
                                 Map<String, Class> indexs, List<JSONObject> data, String table, OpEnum opEnum) {
        Class aClass = indexs.get(table);
        // 构建es索引
        IndexCoordinates indexCoordinates = elasticsearchRestTemplate.getIndexCoordinatesFor(aClass);
        // 获取id字段
        String idPropertyName = null;
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Id.class)!=null) {
                idPropertyName = field.getName();
            }
        }

        // 得到前端跳转的请求参数地址
        String requestUrl = null;
        Annotation annotation = aClass.getAnnotation(RequestUrl.class);
        if (annotation!=null) {
            requestUrl = ((RequestUrl) annotation).requestUrl();
        }

        // 根据不同的crud类型返回不同的数据
        switch (opEnum) {
            case r:
            case c:
                List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
                for (int i = 0; i <data.size(); i++) {
                    data.get(i).put("requestUrl",buildRequestUrl(requestUrl,data.get(i)));
                    IndexQuery indexQuery = new IndexQuery();
                    indexQuery.setId(data.get(i).getString(idPropertyName));
                    indexQuery.setSource(data.get(i).toString(SerializerFeature.WriteDateUseDateFormat));
                    indexQueries.add(indexQuery);
                }
                elasticsearchRestTemplate.bulkIndex(indexQueries,indexCoordinates);
                break;
            case u:
                List<UpdateQuery> updateQueries = new ArrayList<UpdateQuery>();
                for (int i = 0; i <data.size(); i++) {
                    data.get(i).put("requestUrl",buildRequestUrl(requestUrl,data.get(i)));
                    UpdateQuery updateQuery = UpdateQuery.builder(data.get(i).getString(idPropertyName))
                            .withDocAsUpsert(true)
                            .withDocument(Document.parse(data.get(i).toString(SerializerFeature.WriteDateUseDateFormat)))
                            .build();
                    updateQueries.add(updateQuery);
                }
                elasticsearchRestTemplate.bulkUpdate(updateQueries,indexCoordinates);
                break;
            case d:
                for (int i = 0; i <data.size(); i++) {
                    elasticsearchRestTemplate.delete(JSONObject.toJavaObject(data.get(i),aClass));
                }
            default:
        }
    }

    /**
     * @Description 构建执行参数
     * @return buildPropertites
     * @param indexs
     */
    private Properties buildPropertites(Map<String, Class> indexs) {
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
        props.setProperty("database.history.store.only.monitored.tables.ddl.events","false");
        props.setProperty("schema.history.internal", "io.debezium.storage.redis.history.RedisSchemaHistory");
        props.setProperty("schema.history.internal.redis.address", mysqlDataToEsPropertites.getRedisUrl());
        props.setProperty("schema.history.internal.redis.password", mysqlDataToEsPropertites.getRedisPassword());
        props.setProperty("topic.prefix", "my-app-connector");
        props.setProperty("snapshot.mode", "never");
        props.setProperty("database.history.store.only.monitored.tables.ddl.events", "false");
        props.setProperty("database.history.skip.unparseable.ddl", "true");
        props.setProperty("decimal.handling.mode","string");
        // 设置默认即可，但是会存在多项目的情况下serverid偏移的问题 todo
        props.setProperty("database.server.id", "185744");
        props.setProperty("database.include.list", jdbcConnectionInfo.getDatabase());
        // 要捕获的数据表
        Set<String> tables = indexs.keySet();
        StringBuffer sb = new StringBuffer();
        for (String table : tables) {
            sb.append(jdbcConnectionInfo.getDatabase()).append(".").append(table).append(",");
        }
        props.setProperty("table.include.list", sb.toString());
        props.setProperty("database.connectionTimeZone", "UTC");
        props.setProperty("database.server.name", "my-app-connector");
        return props;
    }

    /**
     * 替换请求中的参数信息
     * @param content
     * @param data
     * @return
     */
    private static String buildRequestUrl(String content, JSONObject data){
        if (!StringUtils.hasText(content)){
            return "";
        }
        String pattern = "\\{(.*?)\\}".intern();
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find())
        {
            String key = m.group(1);
            String value = data.getString(key);
            m.appendReplacement(sb, value == null ? "" : value);
        }
        m.appendTail(sb);
        return  sb.toString();
    }

}