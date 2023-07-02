package cn.zflzqy.binlog.source;

import cn.zflzqy.binlog.source.enums.DataTypeEnum;
import cn.zflzqy.binlog.source.impl.KafkaStream;
import cn.zflzqy.binlog.source.impl.MySqlBinlogStream;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.*;

/**
 * @Author: zfl
 * @Date: 2022-07-24-10:49
 * @Description: 流使用策略
 */
public class DataStreamStrategy {
    private static HashMap<String, AbstractDataStreamSourceFactory> streamSourceHashMap = new HashMap<>(4);
    static {
        streamSourceHashMap.put(DataTypeEnum.mysqlBingLog.enumField, new MySqlBinlogStream());
        streamSourceHashMap.put(DataTypeEnum.kafka.enumField, new KafkaStream());
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

            if (config.getJSONObject(i).getBooleanValue("includeSchema",false)) {
                // 对数据做数据库结构处理
                SingleOutputStreamOperator<String> outputStreamOperator = streamSource.flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String s, Collector<String> collector) throws Exception {
                        JSONObject data = JSONObject.parseObject(s);
                        JSONObject payload = data.getJSONObject("payload");
                        if (!payload.containsKey("op")){
                            collector.collect(s);
                        }

                        // before数据结构
                        JSONArray beforeStruct = new JSONArray();
                        // after 数据结构
                        JSONArray afterStruct = new JSONArray();
                        JSONArray fields = data.getJSONObject("schema").getJSONArray("fields");
                        for (int j =0;j<fields.size();j++){
                            if ("before".equals(fields.getJSONObject(j).getString("field"))){
                                beforeStruct = fields.getJSONObject(j).getJSONArray("fields");
                            }else  if ("after".equals(fields.getJSONObject(j).getString("field"))){
                                afterStruct = fields.getJSONObject(j).getJSONArray("fields");
                            }
                        }

                        // 处理数据before
                        formatDate(payload.getJSONObject("before"), beforeStruct);
                        // 处理after数据
                        formatDate(payload.getJSONObject("after"), afterStruct);
                        collector.collect(payload.toString());
                    }
                });
                // 重新赋值
                streamSource = new DataStreamSource<>(outputStreamOperator);
            }
            Tuple2<DataStreamSource<String>,JSONArray> data = new Tuple2<>(streamSource,mappings);
            dataStreamSources.add(data);
        }
        return dataStreamSources;
    }

    /**
     * 转换日期信息
     * @param data : 数据
     * @param structData： 数据结构
     */
    private static void formatDate(JSONObject data, JSONArray structData) throws ParseException {
        if (CollectionUtils.isEmpty(data)||CollectionUtils.isEmpty(structData)){
            return;
        }
        for (int m = 0; m< structData.size(); m++){
            JSONObject struct = structData.getJSONObject(m);
            String name = struct.getString("name");
            if ("io.debezium.time.Timestamp".equals(name)){
                String field = struct.getString("field");
                if (null!=data.getLong(field)) {
                    data.put(field, DateFormatUtils.format(data.getLong(field), "yyyy-MM-dd HH:mm:ss",TimeZone.getTimeZone("GMT")));
                }
            }else if ("io.debezium.time.Date".equals(name)){
                String field = struct.getString("field");
                if (null!=data.getInteger(field)) {
                    Date date = DateUtils.addDays(new Date(0), data.getInteger(field));
                    data.put(field, DateFormatUtils.format(date,"yyyy-MM-dd"));
                }
            }else if ("io.debezium.time.ZonedTimestamp".equals(name)){
                String field = struct.getString("field");
                if (null!=data.getDate(field)) {
                    data.put(field, DateFormatUtils.format(data.getDate(field),"yyyy-MM-dd HH:mm:ss"));
                }
            }
        }
    }
}
