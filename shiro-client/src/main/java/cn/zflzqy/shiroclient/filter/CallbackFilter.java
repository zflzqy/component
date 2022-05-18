package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.util.StrUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        super.doFilter(servletRequest, servletResponse, filterChain);
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
