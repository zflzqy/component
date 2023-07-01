package cn.zflzqy.shiroclient.redis;

import org.crazycake.shiro.IRedisManager;
import org.crazycake.shiro.common.WorkAloneRedisManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisManager extends WorkAloneRedisManager implements IRedisManager {
    private static final String DEFAULT_HOST = "127.0.0.1:6379";
    private String host = "127.0.0.1:6379";
    private int timeout = 2000;
    private String password;
    private int database = 0;
    private JedisPool jedisPool;

    public RedisManager() {
    }

    private void init() {
        if (this.jedisPool == null) {
            Class var1 = RedisManager.class;
            synchronized(RedisManager.class) {
                if (this.jedisPool == null) {
                    String[] hostAndPort = this.host.split(":");
                    this.jedisPool = new JedisPool(this.getJedisPoolConfig(), hostAndPort[0], Integer.parseInt(hostAndPort[1]), this.timeout, this.password, this.database);
                }
            }
        }

    }


    @Override
    public byte[] set(byte[] key, byte[] value, int expireTime) {
        if (key == null) {
            return null;
        } else {
            Jedis jedis = null;
            try {
                jedis     = this.getJedis();
                if (expireTime > 0) {
                    jedis.setex(key,Long.valueOf(expireTime),value);
                }else {
                    jedis.set(key, value);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                jedis.close();
            }

            return value;
        }
    }

    @Override
    protected Jedis getJedis() {
        if (this.jedisPool == null) {
            this.init();
        }

        return this.jedisPool.getResource();
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return this.database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public JedisPool getJedisPool() {
        return this.jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
}
