package cn.zflzqy.mysqldatatoes.thread;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: zfl
 * @Date: 2023-07-02-9:41
 * @Description:
 */
public class CheckApp implements Runnable{
    // 过期时间（秒）
    private static final int EXPIRE_TIME = 60;
    // 当前应用的IP和端口
    private static String IP_PORT = null;
    public static boolean getLock = false;
    private String appName;
    private String port;
    private JedisPool jedisPool;

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

    public CheckApp(String appName, String port,JedisPool jedisPool) {
        this.appName = appName;
        this.port = port;
        this.jedisPool = jedisPool;
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
        IP_PORT = ip + port;
    }

    @Override
    public void run() {
        // 写入redis进度
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "esToMysqlData::"+appName;
            while (true) {
                if (jedis.setnx(key, IP_PORT)==1) {
                    // 成功获取到锁
                    getLock = true;
                }
                // 更新过期时间
                if (jedis.get(key).equals(IP_PORT)) {
                    jedis.expire(key, EXPIRE_TIME);
                }

                try {
                    Thread.sleep(EXPIRE_TIME/2 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
