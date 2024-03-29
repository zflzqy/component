package cn.zflzqy.readMysqlBinlog.sink;

import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:09
 * @Description:
 */
public interface SinkStrategy {

    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource);
}
