package cn.zflzqy.binlog.transform.strategy;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:09
 * @Description:
 */
public interface SinkStrategy {

    /**
     * 执行策略
     * @param config：配置信息
     * @param dataStreamSource：数据源
     */
    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource);
}
