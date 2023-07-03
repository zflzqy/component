package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import cn.zflzqy.shiroclient.config.ShiroRedisProperties;
import cn.zflzqy.shiroclient.shiro.ShiroSessionManager;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @Author: zfl
 * @Date: 2022-03-31-7:43
 * @Description: 向cas发起登录请求
 */
public class SecurityFilter extends io.buji.pac4j.filter.SecurityFilter {
    private StringRedisTemplate stringRedisTemplate;
    private ShiroRedisProperties shiroRedisProperties;
    /**
     * token模式重定向地址
     */
    private static String tokenRedirectUrl;

    public SecurityFilter(StringRedisTemplate stringRedisTemplate, ShiroRedisProperties shiroRedisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.shiroRedisProperties = shiroRedisProperties;
        this.buildTokenUlr();
    }

    public SecurityFilter() {
        this.buildTokenUlr();
    }

    /**
     * 构建token模式下的重定向url
     */
    public void buildTokenUlr() {
        Map<String, String> param = MapUtil.newHashMap(5);
        if (shiroRedisProperties != null && ShiroRedisProperties.TOKEN.equals(shiroRedisProperties.getMode())) {
            param.put("response_type", "code");
            param.put("client_id", shiroRedisProperties.getClientId());
            param.put("redirect_uri", StrUtil.removeSuffix(shiroRedisProperties.getCallbackUrl(), "/") + ShiroRedisProperties.CALLBACK_LOGIN_PATH);
            tokenRedirectUrl = StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl(), "/") + "oauth2.0/authorize?" + HttpUtil.toParams(param);
        }
    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
        // token模式需要续费accessToken,同时需要校验是否需要登录权限
        if (!ShiroRedisProperties.TOKEN.equals(shiroRedisProperties.getMode())) {
            super.doFilter(servletRequest, servletResponse, filterChain);
            // 用户会话
            String session = SecurityUtils.getSubject().getSession().getId().toString();
            // 刷新会话缓存信息
            if (stringRedisTemplate == null || StrUtil.isBlank(session)) {
                return;
            }

            // 获取st
            String st = stringRedisTemplate.opsForValue().get(CallbackFilter.SESSION_ST_KEY + session);
            // 刷新st票据缓存信息
            if (StrUtil.isNotBlank(st)) {
                // 刷新缓存过期时间
                stringRedisTemplate.expire(CallbackFilter.SESSION_ST_KEY + session, ShiroConfig.EXPIRE, TimeUnit.SECONDS);
                stringRedisTemplate.expire(CallbackFilter.ST_KEY + st, ShiroConfig.EXPIRE, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.delete(ShiroConfig.SESSION_KEY + session);
            }

        } else {
            // 地址匹配
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String token = request.getHeader(ShiroSessionManager.AUTHORIZATION);
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            // 获取用户信息
            String userStr = stringRedisTemplate.opsForValue().get(CallbackFilter.TOKEN + token);
            if (StrUtil.isBlank(userStr)) {
                response.sendRedirect(tokenRedirectUrl);
                return;
            }

            // 解析用户
            JSONObject userInfo = JSONUtil.parseObj(userStr);
            if (userInfo.isEmpty()) {
                response.sendRedirect(tokenRedirectUrl);
                return;
            }
            // token续期
            stringRedisTemplate.expire(CallbackFilter.TOKEN + token, userInfo.getJSONObject("accessTokenInfo").getLong("expires_in"), TimeUnit.SECONDS);

        }
    }
}
