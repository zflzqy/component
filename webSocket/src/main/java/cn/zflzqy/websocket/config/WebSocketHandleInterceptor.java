package cn.zflzqy.websocket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

public class WebSocketHandleInterceptor implements ChannelInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandleInterceptor.class);
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            Principal principal = accessor.getUser();
            LOGGER.info("ws连接用户信息：{}",principal.toString());
            accessor.setUser(principal);
        }
        return message;
    }
}