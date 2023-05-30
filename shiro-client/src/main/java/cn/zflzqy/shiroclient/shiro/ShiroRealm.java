package cn.zflzqy.shiroclient.shiro;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @Description Shiro权限匹配和账号密码匹配
 */
public class ShiroRealm extends Pac4jRealm {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroRealm.class);

    /** 重写缓存清理方法*/
    @Override
    protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
        return  SecurityUtils.getSubject().getSession().getId();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // 父级获取验证数据,此处存储cas验证会话信息
        AuthenticationInfo authenticationInfo = super.doGetAuthenticationInfo(token);
        return authenticationInfo;
    }

    /**
     * 授权权限
     * 用户进行权限验证时候Shiro会去缓存中找,如果查不到数据,会执行这个方法去查权限,并放入缓存中
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Map<String, Object> attributes = ((Pac4jPrincipal) principals.asList().get(0)).getProfile().getAttributes();
        LOGGER.info("获取到的用户属性: {}", JSONUtil.toJsonStr(attributes));
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        // 用户的权限信息
        Object authoritiesObj = attributes.get("auths");
        if (authoritiesObj != null) {
            // 权限数组
            List<String> authoritiesArr = new ArrayList<>();
            int length;
            int i;
            String authority;
            if (authoritiesObj instanceof String) {
                // 字符串
                authoritiesArr = StrUtil.splitTrim((String) authoritiesObj, ",");
            } else if (authoritiesObj instanceof Collection) {
                // 集合
                Collection<Object> collection = (Collection<Object>) authoritiesObj;
                for (Object obj : collection) {
                    // 将元素转换为 String 类型
                    String str = obj.toString();
                    authoritiesArr.add(str);
                }
            } else if (authoritiesObj instanceof String[]) {
                // 字符串数组
                authoritiesArr = new ArrayList<>(Arrays.asList((String[])authoritiesObj));
            }
            length = authoritiesArr.size();
            for(i = 0; i < length; ++i) {
                authority = authoritiesArr.get(i);
                info.addStringPermission(authority);
            }
        }

        return info;
    }

}
