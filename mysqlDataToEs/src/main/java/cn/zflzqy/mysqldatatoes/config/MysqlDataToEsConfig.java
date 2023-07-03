package cn.zflzqy.mysqldatatoes.config;

import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import cn.zflzqy.mysqldatatoes.execute.SyncDatatExcute;
import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import cn.zflzqy.mysqldatatoes.handler.TransDateHandler;
import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.thread.CheckApp;
import cn.zflzqy.mysqldatatoes.thread.ThreadPoolFactory;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import cn.zflzqy.mysqldatatoes.util.JedisPoolUtil;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(MysqlDataToEsPropertites.class)
public class MysqlDataToEsConfig {

    private static final Logger log = LoggerFactory.getLogger(MysqlDataToEsConfig.class);
    // 检测app是否正确的应用
    private ThreadPoolExecutor checkAppPoolExecutor;

    public static final String SLAVE_ID_KEY = "esToMysqlData::slave::id::key";

    @Autowired
    private Environment environment;

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
        // 初始化redis客户端
        JedisPoolUtil.initialize(mysqlDataToEsPropertites);
        JedisPool jedisPool = JedisPoolUtil.getInstance();

        // 扫描实体
        PackageScan.scanEntities(StringUtils.hasText(mysqlDataToEsPropertites.getBasePackage())?
                mysqlDataToEsPropertites.getBasePackage():
                PackagePathResolver.mainClassPackagePath);
        Map<String, Class> indexs = PackageScan.getIndexs();

        // 构建执行参数
        Properties props = buildPropertites(indexs,jedisPool);

        // 获取应用名称，标识集群下的唯一性
        String springName = environment.getProperty("spring.application.name");

        // 每间隔30s检测当前应用是否存活，且标志位为当前应用，
        checkAppPoolExecutor = ThreadPoolFactory.build("mysql-data-to-es-check");
        checkAppPoolExecutor.execute(new CheckApp(springName,environment.getProperty("server.port"),jedisPool,elasticsearchRestTemplate,props));

    }

    @PreDestroy
    private void destroy() throws Exception {
        if (checkAppPoolExecutor!=null){
            checkAppPoolExecutor.shutdown();
        }
        if (JedisPoolUtil.getInstance()!=null){
            // 销毁连接池
            JedisPoolUtil.getInstance().destroy();
        }
    }

    /**
     * @Description 构建执行参数
     * @return buildPropertites
     * @param indexs
     */
    private Properties buildPropertites(Map<String, Class> indexs,JedisPool jedisPool) {
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
        props.setProperty("database.connectionTimeZone", "UTC");
        props.setProperty("database.server.name", "my-app-connector");
        props.setProperty("database.include.list", jdbcConnectionInfo.getDatabase());

        // 设置默认即可，但是会存在多项目的情况下serverid偏移的问题
        long serverId = 100000L;
        try (Jedis jedis = jedisPool.getResource()) {
            // 如果不存在key则设置起始值为100000
            boolean exists = jedis.exists(SLAVE_ID_KEY);
            if (exists||jedis.setnx(SLAVE_ID_KEY, "100000")==0) {
                // 获取自增的值
                serverId = jedis.incr(SLAVE_ID_KEY);
            }
        }
        props.setProperty("database.server.id", String.valueOf(serverId));

        // 要捕获的数据表
        if (StringUtils.hasText(mysqlDataToEsPropertites.getIncludeTables())){
            props.setProperty("table.include.list", mysqlDataToEsPropertites.getIncludeTables());
        }else {
            Set<String> tables = indexs.keySet();
            StringBuffer sb = new StringBuffer();
            for (String table : tables) {
                sb.append(jdbcConnectionInfo.getDatabase()).append(".").append(table).append(",");
            }
            props.setProperty("table.include.list", sb.toString());
        }
        return props;
    }


}