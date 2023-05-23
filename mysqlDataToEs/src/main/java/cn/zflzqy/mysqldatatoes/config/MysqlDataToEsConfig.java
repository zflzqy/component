package cn.zflzqy.mysqldatatoes.config;

import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.thread.ThreadPoolFactory;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(MysqlDataToEsPropertites.class)
public class MysqlDataToEsConfig  {
    private static final Logger log = LoggerFactory.getLogger(MysqlDataToEsConfig.class);

    private static DebeziumEngine<ChangeEvent<String, String>> engine;

    private static ThreadPoolExecutor poolExecutor = ThreadPoolFactory.build();

    @Autowired
    private MysqlDataToEsPropertites mysqlDataToEsPropertites;

    @PostConstruct
    public void start() throws Exception {
        // Define the configuration for the Debezium Engine with MySQL connector...
        final Properties props = new Properties();
        props.setProperty("name", "engine");
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        // redis记录偏移量
        props.setProperty("offset.storage", "io.debezium.storage.redis.offset.RedisOffsetBackingStore");
        props.setProperty("offset.storage.redis.address", "127.0.0.1:6379");
        props.setProperty("offset.storage.redis.password", "123456");
        props.setProperty("offset.flush.interval.ms", "10000");
        /* begin connector properties */
        props.setProperty("database.hostname", "localhost");
        props.setProperty("database.port", "3306");
        props.setProperty("database.user", "root");
//        MemorySchemaHistory
//        io.debezium.relational.history.MemorySchemaHistory
        props.setProperty("schema.history.internal", "io.debezium.storage.redis.history.RedisSchemaHistory");
        props.setProperty("schema.history.internal.redis.address","127.0.0.1:6379");
        props.setProperty("schema.history.internal.redis.password","123456");
        props.setProperty("database.password", "123456");
        props.setProperty("topic.prefix", "my-app-connector");
        // 设置默认即可，但是会存在多项目的情况下serverid偏移的问题 todo
        props.setProperty("database.server.id", "85744");
        //
        props.setProperty("database.include.list","myapp");
        props.setProperty("table.include.list", "myapp.crawler_config");//要捕获的数据表

        props.setProperty("database.serverTimezone", "Asia/Shanghai");
        props.setProperty("database.connectionTimeZone", "Asia/Shanghai");
        props.setProperty("database.server.name", "my-app-connector");

        // Create the engine with this configuration ...
        try (DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(record -> {
                    System.out.println(record);
                }).build()
        ) {
            // Run the engine asynchronously ...
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(engine);

            // Do something else or wait for a signal or an event
        }
    }

    @PreDestroy
    private void destory() throws Exception {
        if (engine!=null) {
            engine.close();
        }
        if (poolExecutor!=null) {
            poolExecutor.shutdown();
        }
    }

}