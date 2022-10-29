package cn.zflzqy.binlog.source.impl;

import cn.zflzqy.binlog.source.AbstractDataStreamSourceFactory;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author: zfl
 * @Date: 2022-07-24-10:56
 * @Description:
 */
public class KafkaStream extends AbstractDataStreamSourceFactory {
    @Override
    protected DataStreamSource<String> getStream(JSONObject config, StreamExecutionEnvironment streamExecutionEnvironment) {
        return null;
    }
}
