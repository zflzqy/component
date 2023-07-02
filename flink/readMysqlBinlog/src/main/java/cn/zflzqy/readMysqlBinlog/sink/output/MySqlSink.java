package cn.zflzqy.readMysqlBinlog.sink.output;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.sink.SinkStrategy;
import cn.zflzqy.readMysqlBinlog.sink.componet.JdbcTemplateSink;
import cn.zflzqy.readMysqlBinlog.sink.enums.OpEnum;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Author: zfl
 * @Date: 2022-07-25-21:07
 * @Description:
 */
public class MySqlSink implements SinkStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlSink.class);

    @Override
    public void doOperation(JSONObject config, DataStreamSource<String> dataStreamSource) {
        // 构建连接池
        DataBase dataBase = new DataBase();
        String ip = config.getString("ip");
        int port = config.getInteger("port");
        String dataBaseName = config.getString("dataBaseName");

        dataBase.setIp(ip);
        dataBase.setPort(port);
        dataBase.setDatabaseName(dataBaseName);
        dataBase.setUsername(config.getString("username"));
        dataBase.setPassword(config.getString("password"));
        // 流复制  多表
        JSONArray tables = config.getJSONArray("tables");
        for (int j = 0; j < tables.size(); j++) {
            OutputTag<Object> outputTag = new OutputTag<Object>(StringUtils.join(new String[]{"mysqlSink", ip, String.valueOf(port), dataBaseName}, j, ":")) {
            };
            // 获取表配置
            JSONObject tablesJSONObject = tables.getJSONObject(j);
            dataStreamSource
                    .flatMap(new FlatMapFunction<String, Object>() {
                        @Override
                        public void flatMap(String s, Collector<Object> collector) {
                            // 此次将类型处理为
                            LOGGER.info("将{}处理到{}.{},映射关系：{}", s, dataBaseName, tablesJSONObject.getString("table"), tablesJSONObject.getString("columnMappings"));
                            // 最终形成sql语句
                            JSONObject data = JSONObject.parseObject(s);
                            // 主键
                            String idColumn = tablesJSONObject.getString("tableId");
                            // 类型
                            List<Tuple2<String, List<Object>>> sqls = OpEnum.valueOf(data.getString("op")).doOp(data, idColumn,
                                    tablesJSONObject.getString("table"), tablesJSONObject.getJSONObject("columnMappings"));
                            Tuple2<String, List<Tuple2<String, List<Object>>>> rs = new Tuple2<>(data.getString("op"), sqls);
                            if (!CollectionUtils.isEmpty(sqls)) {
                                collector.collect(rs);
                            }

                        }
                    })
                    .process(new ProcessFunction<Object, Object>() {
                        @Override
                        public void processElement(Object s, ProcessFunction<Object, Object>.Context context, Collector<Object> collector){
                            collector.collect(s);
                            context.output(outputTag, s);
                        }
                    })
                    .getSideOutput(outputTag)
                    .addSink(new JdbcTemplateSink(dataBase));

        }
    }
}
