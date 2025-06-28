package cn.zflzqy.shiroclient.filter;

import cn.hutool.core.util.StrUtil;
import cn.zflzqy.shiroclient.config.ShiroConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;


/***
* @description:登出服务器
* @author: zfl
* @return:
* @param:  * @param null
* @time: 2022/5/22 9:47
 */
public class LogoutFilter extends io.buji.pac4j.filter.LogoutFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutFilter.class);
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String logoutRequest = servletRequest.getParameter("logoutRequest");
        if (StrUtil.isNotBlank(logoutRequest)){
            // 获取st验证票据并删除
            String st = extractSessionIndex(logoutRequest);
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

    public static String extractSessionIndex(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // 关键：启用命名空间支持
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            NodeList nodeList = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "SessionIndex");
            if (nodeList.getLength() > 0) {
                return nodeList.item(0).getTextContent();
            }
        } catch (Exception e) {
            log.error("Error extracting SessionIndex from XML: {}", e.getMessage(), e);
        }
        return null;
    }

    public StringRedisTemplate getStringRedisTemplate() {
        return stringRedisTemplate;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
}
