package cn.zflzqy.mysqldatatoes.propertites;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Author: zfl
 * @Date: 2023-05-22-17:20
 * @Description:
 */
@ConfigurationProperties(prefix = "zfl.zqy.mysqltoes")
@Component
public class MysqlDataToEsPropertites {
    @Autowired
    private DataSourceProperties dataSourceProperties;
    @Autowired
    private RedisProperties redisProperties;

    /** mysql连接地址*/
    private String mysqlUrl;
    /** 用户名*/
    private String mysqlUsername;
    /** 密码*/
    private String mysqlPassword;

    /** redis连接地址*/
    private String redisUrl;
    /** 密码*/
    private String redisPassword;
    /** 数据库*/
    private Integer redisDatabase;

    /** 扫描包路径*/
    private String basePackage;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getMysqlUrl() {
        if (!StringUtils.hasText(mysqlUrl)){
            return dataSourceProperties.getUrl();
        }
        return mysqlUrl;
    }

    public void setMysqlUrl(String mysqlUrl) {
        this.mysqlUrl = mysqlUrl;
    }

    public String getMysqlUsername() {
        if (!StringUtils.hasText(mysqlUsername)){
            return dataSourceProperties.getUsername();
        }
        return mysqlUsername;
    }

    public void setMysqlUsername(String mysqlUsername) {
        this.mysqlUsername = mysqlUsername;
    }

    public String getMysqlPassword() {
        if (!StringUtils.hasText(mysqlPassword)){
            return dataSourceProperties.getPassword();
        }
        return mysqlPassword;
    }

    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }

    public String getRedisUrl() {
        if (!StringUtils.hasText(redisUrl)){
            return redisProperties.getHost()+":"+redisProperties.getPort();
        }
        return redisUrl;
    }

    public void setRedisUrl(String redisUrl) {
        this.redisUrl = redisUrl;
    }

    public String getRedisPassword() {
        if (!StringUtils.hasText(redisPassword)){
            return redisProperties.getPassword();
        }
        return redisPassword;
    }

    public void setRedisPassword(String redisPassword) {
        this.redisPassword = redisPassword;
    }

    public int getRedisDatabase() {
        if (redisDatabase!=null){
            return redisProperties.getDatabase();
        }
        return redisDatabase;
    }

    public void setRedisDatabase(int redisDatabase) {
        this.redisDatabase = redisDatabase;
    }
}
