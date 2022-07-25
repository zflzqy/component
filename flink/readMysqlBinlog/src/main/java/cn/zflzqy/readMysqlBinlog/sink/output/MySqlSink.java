package cn.zflzqy.readMysqlBinlog.sink.output;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.sink.SinkStrategy;
import cn.zflzqy.readMysqlBinlog.sink.componet.JdbcTemplateSink;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:07
 * @Description:
 */
public class MySqlSink  implements SinkStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlSink.class);
    @Override
    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource) {
        // 构建连接池
        DataBase dataBase =new DataBase();
        String ip = config.getString("ip");
        int port = config.getInteger("port");
        String dataBaseName = config.getString("dataBaseName");

        dataBase.setIp(ip);
        dataBase.setPort(port);
        dataBase.setDatabaseName(dataBaseName);
        dataBase.setUsername(config.getString("username"));
        dataBase.setPassword(config.getString("password"));
        // 流复制 todo 多表
        JSONArray tables = config.getJSONArray("tables");
        for (int j=0;j<tables.size();j++) {
            OutputTag<String> outputTag = new OutputTag<String>(StringUtils.join(new String[]{"mysqlSink",ip, String.valueOf(port),dataBaseName},j,":"), Types.STRING) {};
            // 获取表配置
            JSONObject tablesJSONObject = tables.getJSONObject(j);
            SingleOutputStreamOperator<String> streamOperator = dataStreamSource.process(new ProcessFunction<String, String>() {
                @Override
                public void processElement(String s, ProcessFunction<String, String>.Context context, Collector<String> collector) throws Exception {
                    collector.collect(s);
                    context.output(outputTag, s);
                }
            });
            streamOperator.flatMap(new FlatMapFunction<String, Object>() {
                @Override
                public void flatMap(String s, Collector<Object> collector) throws Exception {
                    // 此次将类型处理为
                    LOGGER.info("将{}处理到{}.{},映射关系：{}",s,dataBaseName,tablesJSONObject.getString("table"),tablesJSONObject.getString("columnMappings"));
                    // 最终形成sql语句 todo
                }
            });
            DataStream<String> sideOutput = streamOperator.getSideOutput(outputTag);
            sideOutput.addSink(new JdbcTemplateSink(dataBase));
        }
    }
}
