package cn.zflzqy.mysqldatatoes;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import cn.zflzqy.mysqldatatoes.handler.TransDateHandler;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.springframework.util.StringUtils;

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
                    String value = record.value();
                    if (!StringUtils.hasText(value)) {
                        return;
                    }
                    // 创建 Gson 对象
                    Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

                    // 将字符串转换为 JsonObject
                    JsonObject jsonObject = gson.fromJson(value, JsonObject.class);
                    JsonObject payload = jsonObject.getAsJsonObject("payload");
                    JsonObject source = payload.getAsJsonObject("source");
                    HandlerService handlerService = new TransDateHandler();
                    handlerService.execute(jsonObject);
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