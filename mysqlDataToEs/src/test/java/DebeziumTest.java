import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import com.alibaba.fastjson.JSONObject;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @date 2022/07/29
 */
public class DebeziumTest {

    private static DebeziumEngine<ChangeEvent<String, String>> engine;

    public static void main(String[] args) throws Exception {
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
        props.setProperty("database.history.skip","true");

        props.setProperty("schema.history.internal.redis.address","127.0.0.1:6379");
        props.setProperty("schema.history.internal.redis.password","123456");
        props.setProperty("database.password", "123456");
        props.setProperty("topic.prefix", "my-app-connector");
        // 设置默认即可，但是会存在多项目的情况下serverid偏移的问题 todo
        props.setProperty("database.server.id", "85744");
        //
        props.setProperty("database.include.list","myapp");
        props.setProperty("table.include.list", "fail_crawler_log");//要捕获的数据表
//        props.setProperty("database.serverTimezone", "Asia/Shanghai");
        props.setProperty("database.connectionTimeZone", "UTC");
//        props.setProperty("database.timeZone", "Asia/Shanghai");
        props.setProperty("database.server.name", "my-app-connector");
        props.setProperty("snapshot.mode", "never");
        props.setProperty("schema.history.internal.store.only.captured.databases.ddl", "true");
        props.setProperty("schema.history.internal.store.only.captured.tables.ddl","true");
        Execute execute = new Execute(HandlerEnum.INCREMENTAL);

    // Create the engine with this configuration ...
        try (DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(record -> {
                    System.out.println(record);
                    String value = record.value();
                    if (!StringUtils.hasText(value)) {
                        return;
                    }
                    // 将字符串转换为 JsonObject
                    JSONObject jsonObject = JSONObject.parseObject(value);
                    JSONObject payload = jsonObject.getJSONObject("payload");
                    JSONObject source = payload.getJSONObject("source");
                    String table = source.getString("table");
                    try {

                        execute.execute(jsonObject,DebeziumTest.class);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    System.out.println(jsonObject.toString());
                }).build()
        ) {
            // Run the engine asynchronously ...
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(engine);

            // Do something else or wait for a signal or an event
        }
    }

    private static void closeEngine(DebeziumEngine<ChangeEvent<String, String>> engine) {
        try {
            engine.close();
        } catch (IOException ignored) {
        }
    }

    private static void addShutdownHook(DebeziumEngine<ChangeEvent<String, String>> engine) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeEngine(engine)));
    }

    private static void awaitTermination(ExecutorService executor) {
        if (executor != null) {
            try {
                executor.shutdown();
                while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}