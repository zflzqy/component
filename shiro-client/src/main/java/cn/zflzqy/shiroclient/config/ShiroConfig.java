package cn.zflzqy.shiroclient.config;

import cn.hutool.core.util.StrUtil;
import cn.zflzqy.shiroclient.controller.AuthorizationController;
import cn.zflzqy.shiroclient.filter.CallbackFilter;
import cn.zflzqy.shiroclient.filter.LogoutFilter;
import cn.zflzqy.shiroclient.filter.SecurityFilter;
import cn.zflzqy.shiroclient.shiro.ShiroRealm;
import cn.zflzqy.shiroclient.shiro.ShiroSessionIdGenerator;
import cn.zflzqy.shiroclient.shiro.ShiroSessionManager;
import io.buji.pac4j.context.ShiroSessionStore;
import io.buji.pac4j.subject.Pac4jSubjectFactory;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.config.Config;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.servlet.Filter;
import java.util.*;

/**
 * @Description Shiro配置类
 */
@Configuration
@EnableConfigurationProperties(ShiroRedisProperties.class)
public class ShiroConfig {

    public final static String CACHE_KEY = "shiro:cache:";
    public final static String SESSION_KEY = "shiro:session:";
    /**
     * 过期时间，s（秒）
     */
    public static final int EXPIRE = 1800;

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.timeout}")
    private int timeout;
    @Value("${spring.redis.database}")
    private int database;
    @Value("${spring.redis.password}")
    private String password;
    @Autowired
    private ShiroRedisProperties shiroRedisProperties;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 开启Shiro-aop注解支持
     *
     * @Attention 使用代理方式所以需要开启代码支持
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor(@Qualifier("securityManager") DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);
        return advisorAutoProxyCreator;
    }

    /**
     * Shiro基础配置
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactory(@Qualifier("securityManager") DefaultWebSecurityManager securityManager,Config config) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        shiroFilterFactoryBean.setLoginUrl(shiroRedisProperties.getLoginUrl());
        shiroFilterFactoryBean.setSuccessUrl("/");
        Map<String, Filter> filters = new HashMap(3);
        //cas 认证后回调拦截器
        CallbackFilter callbackFilter = new CallbackFilter();
        callbackFilter.setConfig(config);
        callbackFilter.setStringRedisTemplate(stringRedisTemplate);
        callbackFilter.setShiroRedisProperties(shiroRedisProperties);
        callbackFilter.setDefaultUrl(shiroRedisProperties.getCallbackUrl());
        filters.put("callbackFilter", callbackFilter);
        // 登出拦截器，cas会登出后访问所有登录过的系统的这个地址
        LogoutFilter logoutFilter = new LogoutFilter();
        logoutFilter.setConfig(config);
        logoutFilter.setCentralLogout(true);
        logoutFilter.setLocalLogout(true);
        logoutFilter.setStringRedisTemplate(stringRedisTemplate);
        logoutFilter.setDefaultUrl(shiroRedisProperties.getLoginUrl());
        filters.put("logoutFilter", logoutFilter);
        //cas 资源认证拦截器
        SecurityFilter securityFilter = new SecurityFilter(stringRedisTemplate,shiroRedisProperties);
        securityFilter.setConfig(config);
        securityFilter.setClients("CasClient");
        filters.put("securityFilter", securityFilter);
        shiroFilterFactoryBean.setFilters(filters);
        // 配置shiro默认登录界面地址，前后端分离中登录界面跳转应由前端路由控制，后台仅返回json数据
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap(3);
        filterChainDefinitionMap.put("/login/cas", "callbackFilter");
        filterChainDefinitionMap.put("/logout/cas", "logoutFilter");
        filterChainDefinitionMap.put("/unauthorized","anon");
        // 注意过滤器配置顺序不能颠倒.配置过滤:不会被拦截的链接
        Optional.ofNullable(shiroRedisProperties.getAnonUrl())
                .orElse(new ArrayList<>())
                .forEach(url -> filterChainDefinitionMap.put(url, "anon"));

        // 表示需要认证才可以访问,就是说需要输入token
        Optional.ofNullable(shiroRedisProperties.getPermitUrl())
                .orElse(new ArrayList<>())
                .forEach(url -> filterChainDefinitionMap.put(url, "securityFilter"));
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    /**
     *  pac4j配置
     */
    @Bean("config")
    public Config config() {
        // 配置cas信息
        CasConfiguration configuration = new CasConfiguration();
        //CAS server登录地址
        if (StrUtil.equals(shiroRedisProperties.getMode(),ShiroRedisProperties.TOKEN)){
            configuration.setLoginUrl(StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl() ,"/")+ "oauth2.0/authorize?response_type=code&client_id="+shiroRedisProperties.getClientId()+
                    "&redirect_uri="+StrUtil.addSuffixIfNot(shiroRedisProperties.getCallbackUrl(),"/")+"login/cas");
        }else {
            configuration.setLoginUrl(StrUtil.addSuffixIfNot(shiroRedisProperties.getCasUrl() ,"/")+ "login");
        }

        //CAS 版本，默认为 CAS30，我们使用的是 CAS20
        configuration.setProtocol(CasProtocol.CAS30);
        configuration.setAcceptAnyProxy(true);
        configuration.setPrefixUrl(shiroRedisProperties.getCasUrl());
        // 设置cas信息
        CasClient casClient = new CasClient(configuration);
        //客户端回调地址
        casClient.setCallbackUrl(shiroRedisProperties.getLoginUrl());
        Config config = new Config(casClient);
        // 设置会话存储
        config.setSessionStore(new ShiroSessionStore());
        return config;
    }

    /**
     * 安全管理器
     */
    @Bean
    public DefaultWebSecurityManager securityManager(SessionManager sessionManager, CacheManager cacheManager, ShiroRealm shiroRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 自定义Session管理
        securityManager.setSessionManager(sessionManager);
        // 自定义Cache实现
        securityManager.setCacheManager(cacheManager);
        // 自定义Realm验证
        securityManager.setRealm(shiroRealm);
        // 使用 pac4j 的 subjectFactory
        securityManager.setSubjectFactory(new Pac4jSubjectFactory());
        return securityManager;
    }

    /**
     * 身份验证器
     */
    @Bean
    public ShiroRealm shiroRealm(CacheManager cacheManager) {
        ShiroRealm shiroRealm = new ShiroRealm();
        shiroRealm.setCacheManager(cacheManager);
        return shiroRealm;
    }

    /**
     * 配置Redis管理器
     *
     * @Attention 使用的是shiro-redis开源插件
     */
    @Bean
    public RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(host);
        redisManager.setPort(port);
        redisManager.setTimeout(timeout);
        redisManager.setDatabase(database);
        redisManager.setPassword(password);
        return redisManager;
    }

    /**
     * 配置Cache管理器
     * 用于往Redis存储权限和角色标识
     *
     * @Attention 使用的是shiro-redis开源插件
     */
    @Bean
    public RedisCacheManager cacheManager(RedisManager redisManager) {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager);
        redisCacheManager.setKeyPrefix(CACHE_KEY);
        redisCacheManager.setExpire(EXPIRE);
        return redisCacheManager;
    }

    /**
     * SessionID生成器
     */
    @Bean
    public ShiroSessionIdGenerator sessionIdGenerator() {
        return new ShiroSessionIdGenerator();
    }

    /**
     * 配置RedisSessionDAO
     *
     * @Attention 使用的是shiro-redis开源插件
     */
    @Bean
    public RedisSessionDAO redisSessionDAO(RedisManager redisManager) {
        RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
        redisSessionDAO.setRedisManager(redisManager);
        redisSessionDAO.setKeyPrefix(SESSION_KEY);
        return redisSessionDAO;
    }

    /**
     * 配置Session管理器
     */
    @Bean
    public SessionManager sessionManager(RedisSessionDAO redisSessionDAO) {
        ShiroSessionManager shiroSessionManager = new ShiroSessionManager();
        shiroSessionManager.setSessionDAO(redisSessionDAO);
        return shiroSessionManager;
    }
    @Bean
    public AuthorizationController authorizationController(){
        return  new AuthorizationController();
    }

}
