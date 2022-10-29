package cn.zflzqy.binlog.source;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author: zfl
 * @Date: 2022-07-24-10:44
 * @Description: 数据流构建工厂
 */
public abstract class AbstractDataStreamSourceFactory {
    /**
     * 获取流方法
     * @param config 配置信息
     * @param streamExecutionEnvironment：环境
     * @return
     */
    protected abstract DataStreamSource<String> getStream(JSONObject config,StreamExecutionEnvironment streamExecutionEnvironment);
}
