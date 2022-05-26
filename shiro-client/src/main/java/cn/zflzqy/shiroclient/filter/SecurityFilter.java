package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import cn.zflzqy.shiroclient.config.ShiroRedisProperties;
import cn.zflzqy.shiroclient.shiro.ShiroSessionManager;
import io.buji.pac4j.context.ShiroSessionStore;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.http.adapter.J2ENopHttpActionAdapter;
import org.pac4j.core.util.CommonHelper;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * @Author: zfl
 * @Date: 2022-03-31-7:43
 * @Description: 向cas发起登录请求
 */
public class SecurityFilter extends io.buji.pac4j.filter.SecurityFilter {
    private StringRedisTemplate stringRedisTemplate;
    private ShiroRedisProperties shiroRedisProperties;

    public SecurityFilter(StringRedisTemplate stringRedisTemplate,ShiroRedisProperties shiroRedisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.shiroRedisProperties = shiroRedisProperties;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        // token模式需要续费accessToken,同时需要校验是否需要登录权限 todo
        if (!ShiroRedisProperties.TOKEN.equals(shiroRedisProperties.getMode())) {
            super.doFilter(servletRequest, servletResponse, filterChain);
            // 刷新会话缓存信息
            if (stringRedisTemplate != null) {
                // 刷新会话缓存信息
                String session = SecurityUtils.getSubject().getSession().getId().toString();
                // 获取st
                String st = stringRedisTemplate.opsForValue().get(CallbackFilter.SESSION_ST_KEY + session);
                // 刷新缓存过期时间
                stringRedisTemplate.expire(CallbackFilter.SESSION_ST_KEY + session, ShiroConfig.EXPIRE, TimeUnit.SECONDS);
                stringRedisTemplate.expire(CallbackFilter.ST_KEY + st, ShiroConfig.EXPIRE, TimeUnit.SECONDS);

            }
        }else {
            // 地址匹配
            HttpServletRequest request = (HttpServletRequest)servletRequest;
            String token = request.getHeader(ShiroSessionManager.AUTHORIZATION);
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            String userStr = stringRedisTemplate.opsForValue().get(CallbackFilter.TOKEN+token);
            String redirectUrl =StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl() ,"/")+ "oauth2.0/authorize?response_type=code&client_id="+shiroRedisProperties.getClientId()+
                    "&redirect_uri="+StrUtil.addSuffixIfNot(shiroRedisProperties.getCallbackUrl(),"/")+"login/cas";
            if (StrUtil.isBlank(userStr)){
                response.sendRedirect(redirectUrl);
                return;
            }
            JSONObject userInfo = JSONUtil.parseObj(userStr);
            if (userInfo.isEmpty()){
                response.sendRedirect(redirectUrl);
                return;
            }
            
        }
    }
}
