package cn.zflzqy.readmysqlbinlog.transform.buildSql;

import cn.zflzqy.readmysqlbinlog.model.db.DataBase;
import cn.zflzqy.readmysqlbinlog.transform.strategy.SinkStrategy;
import cn.zflzqy.readmysqlbinlog.sink.db.JdbcTemplateSink;
import cn.zflzqy.readmysqlbinlog.transform.enums.OpEnum;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.api.java.tuple.Tuple2;
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
        String username = config.getString("username");
        String password = config.getString("password");
        dataBase.setIp(ip);
        dataBase.setPort(port);
        dataBase.setDatabaseName(dataBaseName);
        dataBase.setUsername(username);
        dataBase.setPassword(password);
        // 流复制  多表
        JSONArray tables = config.getJSONArray("tables");
        if (CollectionUtils.isEmpty(tables)){
            DataBase newDataBase = new DataBase();
            dataStreamSource
                    .flatMap(new FlatMapFunction<String, Tuple2<DataBase,Object>>() {
                        @Override
                        public void flatMap(String s, Collector<Tuple2<DataBase,Object>> collector) {
                            JSONObject data = JSONObject.parseObject(s);
                            newDataBase.setIp(ip);
                            newDataBase.setPort(port);
                            newDataBase.setUsername(username);
                            newDataBase.setPassword(password);
                            newDataBase.setDatabaseName(dataBaseName);
                            if (StringUtils.isBlank(newDataBase.getDatabaseName())){
                                JSONObject source = data.getJSONObject("source");
                                if (!CollectionUtils.isEmpty(source)) {
                                    newDataBase.setDatabaseName(source.getString("db"));
                                }
                            }
                            // 此次将类型处理为
                            LOGGER.info("将{}处理到{}", s, newDataBase);
                            // 类型
                            String op = data.getString("op");
                            if (StringUtils.isBlank(op)){
                                return;
                            }
                            // 根据类型最终形成sql语句
                            List<Tuple2<String, List<Object>>> sqls = OpEnum.valueOf(op).doOp(data, null,
                                    null,null);
                            Tuple2<String, List<Tuple2<String, List<Object>>>> rs = new Tuple2<>(data.getString("op"), sqls);
                            if (!CollectionUtils.isEmpty(sqls)) {
                                Tuple2<DataBase, Object> out = new Tuple2<>(newDataBase,rs);
                                collector.collect(out);
                            }
                        }
                    })
                    // todo 可以再输出到其他地方
                    .addSink(new JdbcTemplateSink());
        }else {
            for (int j = 0; j < tables.size(); j++) {
                OutputTag<Object> outputTag = new OutputTag<Object>(StringUtils.join(new String[]{"mysqlSink", ip, String.valueOf(port), dataBaseName}, j, ":")) {
                };
                // 获取表配置
                JSONObject tablesJSONObject = tables.getJSONObject(j);
                DataStreamSink dataStreamSink = dataStreamSource
                        .flatMap(new FlatMapFunction<String,  Tuple2<DataBase,Object>>() {
                            @Override
                            public void flatMap(String s, Collector<Tuple2<DataBase,Object>> collector) {
                                // 此次将类型处理为
                                LOGGER.info("将{}处理到{}.{},映射关系：{}", s, dataBaseName, tablesJSONObject.getString("table"), tablesJSONObject.getString("columnMappings"));
                                // 最终形成sql语句
                                JSONObject data = JSONObject.parseObject(s);
                                // 主键
                                String idColumn = tablesJSONObject.getString("tableId");
                                // 类型
                                String op = data.getString("op");
                                if (StringUtils.isBlank(op)){
                                    return;
                                }
                                List<Tuple2<String, List<Object>>> sqls = OpEnum.valueOf(data.getString("op")).doOp(data, idColumn,
                                        tablesJSONObject.getString("table"), tablesJSONObject.getJSONObject("columnMappings"));
                                Tuple2<String, List<Tuple2<String, List<Object>>>> rs = new Tuple2<>(data.getString("op"), sqls);
                                if (!CollectionUtils.isEmpty(sqls)) {
                                    Tuple2<DataBase, Object> out = new Tuple2<>(dataBase,rs);
                                    collector.collect(out);
                                }
                            }
                        })
                        .process(new ProcessFunction<Tuple2<DataBase,Object>, Tuple2<DataBase,Object>>() {
                            @Override
                            public void processElement(Tuple2<DataBase,Object> s, ProcessFunction<Tuple2<DataBase,Object>, Tuple2<DataBase,Object>>.Context context, Collector<Tuple2<DataBase,Object>> collector) {
                                collector.collect(s);
                                context.output(outputTag, s);
                            }
                        })
                        .getSideOutput(outputTag)
                        // todo 可以再输出到其他地方
                        .addSink(new JdbcTemplateSink(dataBase));

            }
        }
    }
}
