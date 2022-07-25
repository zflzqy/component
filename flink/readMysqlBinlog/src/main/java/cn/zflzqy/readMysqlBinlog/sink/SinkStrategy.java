package cn.zflzqy.readMysqlBinlog.sink;

import cn.zflzqy.readMysqlBinlog.sink.output.KafkaSink;
import cn.zflzqy.readMysqlBinlog.sink.output.MySqlSink;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:09
 * @Description:
 */
public interface SinkStrategy {

    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource);
}
