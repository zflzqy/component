package cn.zflzqy.readMysqlBinlog.dataStreamSource;

import cn.zflzqy.readMysqlBinlog.dataStreamSource.impl.KafkaStream;
import cn.zflzqy.readMysqlBinlog.dataStreamSource.impl.MySqlBinlogStream;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: zfl
 * @Date: 2022-07-24-10:49
 * @Description: 流使用策略
 */
public class DataStreamStrategy {
    private static HashMap<String, DataStreamSourceFactory> streamSourceHashMap = new HashMap<>(4);
    static {
        streamSourceHashMap.put("mysql-binlog", new MySqlBinlogStream());
        streamSourceHashMap.put("kafka", new KafkaStream());
    }

    /**
     * 获取流策略
     *
     * @param config：配置信息
     * @return
     */
    public static List<Tuple2<DataStreamSource<String>,JSONArray>> getDataStreamSource(JSONArray config, StreamExecutionEnvironment environment) {
        List<Tuple2<DataStreamSource<String>,JSONArray>> dataStreamSources = new ArrayList<>();
        for (int i = 0; i < config.size(); i++) {
            String type = config.getJSONObject(i).getString("type");
            JSONArray mappings = config.getJSONObject(i).getJSONArray("tableMappings");
            DataStreamSource<String> streamSource = streamSourceHashMap.get(type).getStream(config.getJSONObject(i), environment);
            Tuple2<DataStreamSource<String>,JSONArray> data = new Tuple2<>(streamSource,mappings);
            dataStreamSources.add(data);
        }
        return dataStreamSources;
    }
}
