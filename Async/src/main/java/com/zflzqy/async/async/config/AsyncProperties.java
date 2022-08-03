package com.zflzqy.async.async.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * @Author: zfl
 * @Date: 2022-07-10-10:13
 * @Description: 异步框架
 */
@ConfigurationProperties(prefix = "zfl.zqy.async")
@Component
public class AsyncProperties {
    /** 队列大小*/
    private Integer queueSize = 1024*1024;
    /** 模式*/
    private Class<? extends WaitStrategy> waitStrategy ;

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public Class<? extends WaitStrategy> getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(Class<? extends WaitStrategy> waitStrategy) {
        this.waitStrategy = waitStrategy;
    }
}
