package cn.zflzqy.shiroclient.filter;

import cn.zflzqy.shiroclient.config.ShiroConfig;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * @Author: zfl
 * @Date: 2022-03-31-7:43
 * @Description:
 */
public class SecurityFilter extends io.buji.pac4j.filter.SecurityFilter {
    private StringRedisTemplate stringRedisTemplate;

    public SecurityFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        super.doFilter(servletRequest, servletResponse, filterChain);
        // 刷新会话缓存信息
        if (stringRedisTemplate!=null){
            // 刷新会话缓存信息
            String session = SecurityUtils.getSubject().getSession().getId().toString();
            // 获取st
            String st = stringRedisTemplate.opsForValue().get(CallbackFilter.SESSION_ST_KEY+session);
            // 刷新缓存过期时间
            stringRedisTemplate.expire(CallbackFilter.SESSION_ST_KEY+session, ShiroConfig.EXPIRE,TimeUnit.SECONDS);
            stringRedisTemplate.expire(CallbackFilter.ST_KEY+st,ShiroConfig.EXPIRE,TimeUnit.SECONDS);

        }
    }


}
