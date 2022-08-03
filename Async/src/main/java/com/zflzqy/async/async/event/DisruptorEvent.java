package com.zflzqy.async.async.event;

import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-10-10:36
 * @Description:
 */

public class DisruptorEvent {
    private Map<String,Object> data;
    private DisruptorHandler disruptorHandler;

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public DisruptorHandler getDisruptorHandler() {
        return disruptorHandler;
    }

    public void setDisruptorHandler(DisruptorHandler disruptorHandler) {
        this.disruptorHandler = disruptorHandler;
    }
}
