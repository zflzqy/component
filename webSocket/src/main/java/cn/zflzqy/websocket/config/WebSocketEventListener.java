package cn.zflzqy.websocket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketEventListener {
    private static final ConcurrentHashMap<String, Principal> users = new ConcurrentHashMap<>(8);
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
 
    @EventListener
    public void handleConnectListener(SessionConnectedEvent event) {
        log.info("[ws-connected] socket connect: {}", event.getMessage());
        users.put(event.getUser().getName(),event.getUser());
    }
 
    @EventListener
    public void handleDisconnectListener(SessionDisconnectEvent event) {
        log.info("[ws-disconnect] socket disconnect: {}", event.getMessage());
        users.remove(event.getUser().getName());
    }
    public static  ConcurrentHashMap<String,Principal> getUsers(){
        return users;
    }

}