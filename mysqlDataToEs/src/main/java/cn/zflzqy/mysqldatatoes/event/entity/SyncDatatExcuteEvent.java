
package cn.zflzqy.mysqldatatoes.event.entity;

import org.springframework.context.ApplicationEvent;

public class SyncDatatExcuteEvent extends ApplicationEvent {

        public SyncDatatExcuteEvent(Object source) {
            super(source);
        }
    }