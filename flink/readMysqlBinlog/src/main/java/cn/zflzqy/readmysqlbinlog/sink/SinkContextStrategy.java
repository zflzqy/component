package cn.zflzqy.readmysqlbinlog.sink;

import cn.zflzqy.readmysqlbinlog.sink.output.KafkaSink;
import cn.zflzqy.readmysqlbinlog.sink.output.MySqlSink;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:09
 * @Description:
 */
public class SinkContextStrategy {
    private static Map<String,SinkStrategy> strategyMap = new HashMap<>(2);
    static {
        strategyMap.put("mysql",new MySqlSink());
        strategyMap.put("kafka",new KafkaSink());
    }
    public static void execute(JSONObject config, DataStreamSource<String> dataStreamSource){
        strategyMap.get(config.getString("type")).doOperation(config,dataStreamSource);
    }
    
}
