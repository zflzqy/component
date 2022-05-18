package cn.zflzqy.websocket.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 开启Stomp WebSocket服务
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
 
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //  订阅Broker名称 user点对点 topic广播即群发
        registry.enableSimpleBroker("/topic", "/queue");
        // 设置接收客户端消息 的 路径前缀（不设置可以）
        registry.setApplicationDestinationPrefixes("/app", "/user");
        // 点对点使用的前缀 无需配置 默认/user
        registry.setUserDestinationPrefix("/user");
    }
 
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加拦截器，处理客户端发来的请求
        registration.interceptors(new WebSocketHandleInterceptor());
    }

    /***
    * @description:添加监听器
    * @author: zfl
    * @return: cn.zflzqy.websocket.config.WebSocketEventListener
    * @param:  * @param
    * @time: 2022/5/18 20:12
     */
    @Bean
    public  WebSocketEventListener webSocketEventListener(){
        return  new WebSocketEventListener();
    }
}