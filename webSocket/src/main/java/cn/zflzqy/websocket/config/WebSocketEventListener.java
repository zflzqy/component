package cn.zflzqy.websocket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

public class WebSocketEventListener {
 
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
 
    @EventListener
    public void handleConnectListener(SessionConnectedEvent event) {
        log.info("[ws-connected] socket connect: {}", event.getMessage());
        // do someting ...
    }
 
    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        log.info("[ws-disconnect] socket disconnect: {}", event.getMessage());
        // do someting ...
    }
}