package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import cn.zflzqy.shiroclient.config.ShiroRedisProperties;
import io.buji.pac4j.context.ShiroSessionStore;
import io.buji.pac4j.engine.ShiroCallbackLogic;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.http.adapter.J2ENopHttpActionAdapter;
import org.pac4j.core.util.CommonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
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
 * @description:cas认证成功后的回调过滤器
 * @author: zfl
 * @return:
 * @param: * @param null
 * @time: 2022/5/22 9:47
 */

public class CallbackFilter extends io.buji.pac4j.filter.CallbackFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackFilter.class);
    private StringRedisTemplate stringRedisTemplate;
    private ShiroRedisProperties shiroRedisProperties;
    /**
     * ST验证票据缓存的前缀
     */
    public static final String ST_KEY = "ST-SESSION::";
    /**
     * ST验证票据缓存的前缀
     */
    public static final String SESSION_ST_KEY = "SESSION-ST::";
    /**
     * TOKEN缓存的前缀
     */
    public static final String TOKEN = "TOKEN::";

    private static Map<String,String> getAccessTokenParam;

    public CallbackFilter(StringRedisTemplate stringRedisTemplate, ShiroRedisProperties shiroRedisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.shiroRedisProperties = shiroRedisProperties;
        this.buildParam();
    }

    public CallbackFilter() {
        buildParam();
    }

    private void buildParam() {
        getAccessTokenParam = MapUtil.newHashMap(5);
        getAccessTokenParam.put("grant_type","authorization_code");
        if (shiroRedisProperties!=null) {
            getAccessTokenParam.put("client_id", shiroRedisProperties.getClientId());
            getAccessTokenParam.put("client_secret", shiroRedisProperties.getClientSecret());
            getAccessTokenParam.put("redirect_uri", shiroRedisProperties.getCallbackUrl());
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 判断模式
        if (StrUtil.equals(shiroRedisProperties.getMode(), ShiroRedisProperties.TOKEN)) {
            String code = servletRequest.getParameter("code");
            // 构建获取accessToken地址
            String url = getUrl(code);
            // 获取accessToken
            HttpResponse casResponse = HttpUtil.createGet(url).execute();
            if (casResponse.getStatus()!= HttpStatus.HTTP_OK){
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                response.getWriter().write("获取accessToken失败");
                return;
            }
            String body = casResponse.body();
            JSONObject accessTokenInfo = JSONUtil.parseObj(body);
            String accessToken = accessTokenInfo.getStr("access_token");
            if (StrUtil.isNotBlank(accessToken)) {
                // 获取用户的信息
                casResponse = HttpUtil.createGet(StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl(), "/") + "oauth2.0/profile?access_token=" + accessTokenInfo.getStr("access_token"))
                        .execute();
                body = casResponse.body();
                LOGGER.info("获取的用户信息：{}",body.toString());
                JSONObject userInfo = JSONUtil.parseObj(body);
                // 添加accessTokenInfo信息
                userInfo.putOnce("accessTokenInfo", accessTokenInfo);
                LOGGER.info("获取的用户信息：{}",userInfo.toString());
                // 生成token并存储用户信息到redis中
                String token = JWTUtil.createToken(userInfo,shiroRedisProperties.getClientSecret().getBytes());
                LOGGER.info("获取的token：{}",token);
                stringRedisTemplate.opsForValue().set(TOKEN + token, userInfo.toString(), ShiroConfig.EXPIRE, TimeUnit.SECONDS);
                // 输出到响应体中
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                response.getWriter().write(token);
                response.flushBuffer();
            }
        } else {
            super.doFilter(servletRequest, servletResponse, filterChain);
            // 获取验证票据并存储
            String ticket = servletRequest.getParameter("ticket");
            // 存储st与会话直接的关系
            if (stringRedisTemplate != null && StrUtil.isNotBlank(ticket)) {
                String session = SecurityUtils.getSubject().getSession().getId().toString();
                stringRedisTemplate.opsForValue().set(ST_KEY + ticket, SecurityUtils.getSubject().getSession().getId().toString(),
                        ShiroConfig.EXPIRE, TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().set(SESSION_ST_KEY + session, ticket, ShiroConfig.EXPIRE, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 构建获取accessToken 的url
     * @param code
     * @return
     */
    private String getUrl(String code) {
        Map<String, String> param = ObjectUtil.clone(getAccessTokenParam);
        param.put("code",code);

        return  StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl(), "/") +"oauth2.0/accessToken?"+HttpUtil.toParams(param);
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.buildParam();
    }

    public void setShiroRedisProperties(ShiroRedisProperties shiroRedisProperties) {
        this.shiroRedisProperties = shiroRedisProperties;
        this.buildParam();
    }
}
