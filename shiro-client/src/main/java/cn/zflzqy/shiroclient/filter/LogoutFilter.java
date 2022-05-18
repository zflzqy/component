package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.util.StrUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;


public class LogoutFilter extends io.buji.pac4j.filter.LogoutFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutFilter.class);
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String logoutRequest = servletRequest.getParameter("logoutRequest");
        if (StrUtil.isNotBlank(logoutRequest)){
            // 获取st验证票据并删除
            String st = StrUtil.sub(logoutRequest,logoutRequest.indexOf("<samlp:SessionIndex>")+20,logoutRequest.indexOf("</samlp:SessionIndex>"));
            if (StrUtil.isNotBlank(st)&&stringRedisTemplate!=null){
                // 获取存储的会话
                String session = stringRedisTemplate.opsForValue().get(CallbackFilter.ST_KEY + st);
                // 删除会话数据
                LOGGER.debug("删除会话：{}", ShiroConfig.SESSION_KEY+session);
                stringRedisTemplate.delete(ShiroConfig.SESSION_KEY+session);
                LOGGER.debug("删除st:{}",CallbackFilter.ST_KEY+st);
                stringRedisTemplate.delete(CallbackFilter.ST_KEY+st);
                LOGGER.debug("删除session-st:{}",CallbackFilter.ST_KEY+st);
                stringRedisTemplate.delete(CallbackFilter.SESSION_ST_KEY+session);
            }

        }
        super.doFilter(servletRequest, servletResponse, filterChain);
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
}
