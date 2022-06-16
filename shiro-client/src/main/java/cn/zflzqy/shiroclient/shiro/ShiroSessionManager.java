package cn.zflzqy.shiroclient.shiro;

import cn.hutool.core.util.StrUtil;
import cn.zflzqy.shiroclient.config.ShiroRedisProperties;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.Serializable;

/**
 * @Description 自定义获取Token
 */
public class ShiroSessionManager extends DefaultWebSessionManager {
    @Autowired
    private ShiroRedisProperties shiroRedisProperties;
    //定义常量
    public static final String AUTHORIZATION = "Authorization";
    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";
    //重写构造器
    public ShiroSessionManager() {
        super();
        this.setDeleteInvalidSessions(true);
    }
    /**
     * 重写方法实现从请求头获取Token便于接口统一
     * 每次请求进来,Shiro会去从请求头找Authorization这个key对应的Value(Token)
     */
    @Override
    public Serializable getSessionId(ServletRequest request, ServletResponse response) {
        return  getToken(request,response);
    }
    protected Serializable getToken(ServletRequest request, ServletResponse response) {
        if (StrUtil.equals(shiroRedisProperties.getMode(),ShiroRedisProperties.TOKEN)) {
            String authorization = WebUtils.toHttp(request).getHeader(AUTHORIZATION);
            if (StringUtils.isEmpty(authorization)) {
                // 如果没有携带id参数则按照父类的方式在cookie进行获取
                return null;
            } else {
                // 如果请求头中有 authToken 则其值为sessionId 可以尝试重写
                request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE, REFERENCED_SESSION_ID_SOURCE);
                request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, authorization);
                request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
                return authorization;
            }
        }else {
            // session方式获取
            return super.getSessionId(request, response);
        }
    }
}
