package cn.zflzqy.websocket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * @author ：zfl
 * @description：
 * @date ：2022/3/17 19:35
 */
@ConfigurationProperties(prefix = "zfl.zqy.websocket")
@Component
public class WebSocketProperties {
    /** 当前服务请求头的key*/
    private String  headerKey = "username";

    public String getHeaderKey() {
        return headerKey;
    }

    public void setHeaderKey(String headerKey) {
        this.headerKey = headerKey;
    }
}
