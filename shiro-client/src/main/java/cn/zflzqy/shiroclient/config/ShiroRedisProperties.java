package cn.zflzqy.shiroclient.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author ：zfl
 * @description：
 * @date ：2022/3/17 19:35
 */
@ConfigurationProperties(prefix = "zfl.zqy.shiro.redis")
@Component
public class ShiroRedisProperties {
    /** 默认登录地址后缀*/
    public static final String CALLBACK_LOGIN_PATH = "/login/cas";
    /** 默认登出地址后缀*/
    public static final String CALLBACK_LOGOUT_PATH = "/logout/cas";
    /** 当前服务地址*/
    private String  callbackUrl;
    /** cas地址*/
    private String  casUrl;
    /** 不需要拦截地址*/
    private List<String> anonUrl =  Arrays.asList("/", "/**");;
    /** 需要拦截地址*/
    private List<String> permitUrl;

    /** 登录地址*/
    public String getLoginUrl() {
        return StrUtil.addSuffixIfNot(this.getCallbackUrl(),"/")+"login/cas?client_name=CasClient";
    }
    /** 登出地址*/
    public String getLogoutUrl() {
        return StrUtil.removeSuffix(this.casUrl,"/")+"/logout?service="+StrUtil.removeSuffix(this.callbackUrl,"/")+ CALLBACK_LOGIN_PATH;
    }
    /** cas地址*/
    public String getCallbackLoginUrl() {
        return StrUtil.removeSuffix(this.callbackUrl,"/")+CALLBACK_LOGIN_PATH;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCasUrl() {
        return casUrl;
    }

    public void setCasUrl(String casUrl) {
        this.casUrl = casUrl;
    }

    public List<String> getAnonUrl() {
        return anonUrl;
    }

    public void setAnonUrl(List<String> anonUrl) {
        this.anonUrl = anonUrl;
    }

    public List<String> getPermitUrl() {
        return permitUrl;
    }

    public void setPermitUrl(List<String> permitUrl) {
        this.permitUrl = permitUrl;
    }
}
