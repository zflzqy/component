package com.zflzqy.async.async.event;

import com.lmax.disruptor.EventFactory;

/**
 * @Author: zfl
 * @Date: 2022-07-10-10:35
 * @Description:
 */
public class DisruptorEventFactory implements EventFactory {
    @Override
    public DisruptorEvent newInstance() {
        return new DisruptorEvent();
    }
}
