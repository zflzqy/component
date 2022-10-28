package cn.zflzqy.readmysqlbinlog.dataStreamSource.impl;

import cn.zflzqy.readmysqlbinlog.dataStreamSource.DataStreamSourceFactory;
import com.alibaba.fastjson2.JSONObject;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.connect.json.DecimalFormat;
import org.apache.kafka.connect.json.JsonConverterConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: zfl
 * @Date: 2022-07-24-10:46
 * @Description:
 */
public class MySqlBinlogStream  extends DataStreamSourceFactory {
    /** 服务id */
    private static final AtomicInteger SERVER_IDS = new AtomicInteger(6000);
    @Override
    public DataStreamSource<String> getStream(JSONObject config, StreamExecutionEnvironment env) {
        // ip
        String ip = config.getString("ip");
        // port
        int port = config.getInteger("port");
        // 库名
        String dataBaseName = config.getString("dataBaseName");
        // 表名
        String tableName = config.getString("tableName");
        // 账号
        String username = config.getString("username");
        // 密码
        String password = config.getString("password");

        // 解决BigDecimal序列化异常
        Map<String,Object> serializerConfig = new HashMap(1);
        serializerConfig.put(JsonConverterConfig.DECIMAL_FORMAT_CONFIG, DecimalFormat.NUMERIC.name());
        JsonDebeziumDeserializationSchema jdd = new JsonDebeziumDeserializationSchema(true, serializerConfig);

        // 获取数据源
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname(ip)
                .port(port)
                .databaseList(dataBaseName)
                .tableList(dataBaseName+"."+tableName)
                .username(username)
                .password(password)
                .serverId(String.valueOf(SERVER_IDS.incrementAndGet()))
                .deserializer(jdd)
                .build();

        return env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(),ip+port+dataBaseName+tableName);
    }
}
