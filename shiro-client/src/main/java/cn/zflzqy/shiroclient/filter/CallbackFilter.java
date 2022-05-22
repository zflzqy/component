package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
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
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
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
* @description:cas认证成功后的回调过滤器
* @author: zfl
* @return:
* @param:  * @param null
* @time: 2022/5/22 9:47
 */
public class CallbackFilter extends io.buji.pac4j.filter.CallbackFilter {
    private StringRedisTemplate stringRedisTemplate;
    /** ST验证票据缓存的前缀*/
    public static final String ST_KEY ="ST-SESSION::";
    /** ST验证票据缓存的前缀*/
    public static final String SESSION_ST_KEY ="SESSION-ST::";
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 获取验证票据并存储
        String ticket = servletRequest.getParameter("ticket");
        // 判断模式 toDO
        String code = servletRequest.getParameter("code");

        String url = "https://test.zflzqy.cn:8443/oauth2.0/accessToken?grant_type=authorization_code&client_id=clientId&client_secret=clientId" +
                                     "&code="+code+"&redirect_uri="+"https://test.zflzqy.cn:8080";
        System.out.println();
//        super.doFilter(servletRequest, servletResponse, filterChain);
        // 获取accessToken
        HttpResponse casResponse = HttpUtil.createGet(url).execute();
        String body = casResponse.body();
        JSONObject accessTokenInfo = JSONUtil.parseObj(body);
        String accessToken = accessTokenInfo.getStr("access_token");
        if (StrUtil.isNotBlank(accessToken)){
            // 获取用户的信息
            casResponse = HttpUtil.createGet("https://test.zflzqy.cn:8443/oauth2.0/profile?access_token=" + accessTokenInfo.getStr("access_token"))
                    .execute();
            body = casResponse.body();
            JSONObject userInfo = JSONUtil.parseObj(body);
            // 添加accessTokenInfo信息
            userInfo.putOnce("accessTokenInfo",accessTokenInfo);
            // 存储到redis中

//            JWT token = JWT.create().addPayloads(jsonObject);
        }



//        CommonHelper.assertNotNull("callbackLogic", this.callbackLogic);
//        CommonHelper.assertNotNull("config", this.config);
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        // 重写逻辑输出到客户端，客户端存储token，签名accessToken,其余信息存储到redis中，后续通过redis读取信息
//        JWT token = JWT.create().addPayloads(jsonObject);
//        JWTUtil.createToken()
//        JWTUtil.parseToken()


        // 存储st与会话直接的关系
        if (stringRedisTemplate!=null&&StrUtil.isNotBlank(ticket)) {
            String session = SecurityUtils.getSubject().getSession().getId().toString();
            stringRedisTemplate.opsForValue().set(ST_KEY + ticket, SecurityUtils.getSubject().getSession().getId().toString(),
                    ShiroConfig.EXPIRE, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(SESSION_ST_KEY+session,ticket, ShiroConfig.EXPIRE, TimeUnit.SECONDS);
        }
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
}
