package cn.zflzqy.mysqldatatoes.thread;

import cn.zflzqy.mysqldatatoes.consumer.CustomConsumer;
import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import cn.zflzqy.mysqldatatoes.handler.TransDateHandler;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: zfl
 * @Date: 2023-07-02-9:41
 * @Description:
 */
public class CheckAndRunApp implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CheckAndRunApp.class);
    private static boolean initial = false;
    // 过期时间（秒）
    private static final int EXPIRE_TIME = 60;
    // 当前应用的IP和端口
    private static String IP_PORT = null;

    private String appName;
    private String port;
    private final JedisPool jedisPool;
    private final Execute execute;

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;
    private final Properties props;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public CheckAndRunApp(String appName, String port, JedisPool jedisPool, ElasticsearchRestTemplate elasticsearchRestTemplate, Properties properties) {
        this.appName = appName;
        this.port = port;
        this.jedisPool = jedisPool;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
        this.props = properties;
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
        IP_PORT = ip + port;
        // 数据处理执行类
        this.execute = new Execute(HandlerEnum.INCREMENTAL);
        HandlerService.register(HandlerEnum.INCREMENTAL, new TransDateHandler());
    }

    @Override
    public void run() {
        // 写入redis进度
        String key = "esToMysqlData::" + appName;
        ThreadPoolExecutor poolExecutor = null;
        DebeziumEngine<ChangeEvent<String, String>> engine = null;
        while (true) {
            try (Jedis jedis = jedisPool.getResource()) {

                if (jedis.setnx(key, IP_PORT) == 1) {
                    // 成功获取到锁,创建engine
                    Map<String, Class> indexes = PackageScan.getIndexes();
                    try {
                        log.info("{}获取到了执行器",IP_PORT);
                        engine = DebeziumEngine.create(Json.class)
                                .using(props)
                                .notifying(new CustomConsumer(indexes, execute, elasticsearchRestTemplate))
                                .build();
                        // 构建线程池
                        poolExecutor = new ThreadPoolFactory.ThreadPoolFactoryBuilderImpl()
                                .corePoolSize(1)
                                .maximumPoolSize(1)
                                .keepAliveTime(30L)
                                .prefix("mysql-data-to-es")
                                .build();
                        // 提交任务
                        poolExecutor.execute(engine);
                        initial = true;
                    } catch (Exception e) {
                        log.error("创建 engine 失败", e);
                    }
                }

                // 更新过期时间
                if (IP_PORT.equals(jedis.get(key))&&initial) {
                    jedis.expire(key, EXPIRE_TIME);
                }

                try {
                    log.info("当前机器：{}",IP_PORT);
                    Thread.sleep(EXPIRE_TIME / 2 * 1000);
                } catch (InterruptedException e) {
                    log.error("中断异常：",e);
                }
            }
        }

    }
}
