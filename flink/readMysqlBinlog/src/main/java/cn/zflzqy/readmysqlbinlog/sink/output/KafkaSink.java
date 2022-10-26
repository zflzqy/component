package cn.zflzqy.readmysqlbinlog.sink.output;

import cn.zflzqy.readmysqlbinlog.sink.SinkStrategy;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:07
 * @Description:
 */
public class KafkaSink implements SinkStrategy {
    @Override
    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource) {
        // todo

    }
}
