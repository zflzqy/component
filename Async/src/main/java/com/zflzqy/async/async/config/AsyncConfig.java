package com.zflzqy.async.async.config;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.zflzqy.async.async.event.DisruptorEvent;
import com.zflzqy.async.async.event.DisruptorEventFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: zfl
 * @Date: 2022-07-10-10:31
 * @Description:
 */

@Configuration
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig {
    @Autowired
    private AsyncProperties asyncProperties;

    // 事件
    @Bean
    public EventFactory eventFactory(){
        EventFactory<DisruptorEvent> eventFactory = new DisruptorEventFactory();
        return  eventFactory;
    }

    @Bean
    public Disruptor disruptor(){
        // 创建Disruptor
        Disruptor<DisruptorEvent> disruptor= new Disruptor<DisruptorEvent>(eventFactory(),asyncProperties.getQueueSize(),
                new DaemonThreadFactory("disruptor"), ProducerType.SINGLE,new YieldingWaitStrategy());
//        disruptor.handleEventsWith()
        return  disruptor;
    }
}
