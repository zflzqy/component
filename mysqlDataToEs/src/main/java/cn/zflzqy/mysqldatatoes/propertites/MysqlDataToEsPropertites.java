package cn.zflzqy.mysqldatatoes.propertites;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
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

    /** 连接地址*/
    private String url;
    /** 用户名*/
    private String username;
    /** 密码*/
    private String password;


    public String getUrl() {
        if (!StringUtils.hasText(url)){
            return dataSourceProperties.getUrl();
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        if (!StringUtils.hasText(username)){
            return dataSourceProperties.getUsername();
        }
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if (!StringUtils.hasText(password)){
            return dataSourceProperties.getPassword();
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
