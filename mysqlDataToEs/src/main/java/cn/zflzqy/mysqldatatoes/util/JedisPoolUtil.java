package cn.zflzqy.mysqldatatoes.util;

import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisPoolUtil {
    private static volatile JedisPool instance;

    private JedisPoolUtil() {}

    public static void initialize(MysqlDataToEsPropertites mysqlDataToEsPropertites) {
        if (instance == null) {
            synchronized (JedisPoolUtil.class) {
                if (instance == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    instance = new JedisPool(poolConfig, mysqlDataToEsPropertites.getRedisHost(), mysqlDataToEsPropertites.getRedisPort(), 2000, mysqlDataToEsPropertites.getRedisPassword());
                }
            }
        }
    }

    public static JedisPool getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SingletonJedisPool is not initialized yet. Call initialize(...) method to initialize it.");
        }
        return instance;
    }

}
