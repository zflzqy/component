package cn.zflzqy.readMysqlBinlog.sink.componet;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.pool.DruidPool;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-23-10:57
 * @Description:
 */
public class JdbcTemplateSink<IN> extends RichSinkFunction<IN> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTemplateSink.class);
    private JdbcTemplate jdbcTemplate;
    // jdbc事务编程
    private TransactionTemplate transactionTemplate;
    private DataBase dataBase;

    public JdbcTemplateSink(DataBase dataBase) {
        this.dataBase = dataBase;
    }

    public void open(Configuration parameters) throws Exception {
        jdbcTemplate = new JdbcTemplate();
        DataSource dataSource = DruidPool.getDataSource(dataBase);
        jdbcTemplate.setDataSource(dataSource);
        // 事务构建
        transactionTemplate = new TransactionTemplate();
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionTemplate.setTransactionManager(transactionManager);
        LOGGER.info("获取数据库连接池：{},构建jdbcTemplate:{}",dataSource.hashCode(),jdbcTemplate.hashCode());
        super.open(parameters);
    }
    @Override
    public void invoke(IN value, Context context) throws Exception {
        super.invoke(value, context);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {

                Tuple2<String,List<Tuple2<String, List<Object>>>> executeSql = (Tuple2<String,List<Tuple2<String, List<Object>>>>) value;
                try {
                    if (StringUtils.equals(executeSql.f0,"d")||StringUtils.equals(executeSql.f0,"u")){
                        for (int i=0;i<executeSql.f1.size();i++){
                            jdbcTemplate.update(executeSql.f1.get(i).f0,executeSql.f1.get(i).f1.toArray());
                        }
                    }else {
                        List<Map<String, Object>> list = jdbcTemplate.queryForList(executeSql.f1.get(0).f0, executeSql.f1.get(0).f1.toArray());
                        if (CollectionUtils.isEmpty(list)){
                            jdbcTemplate.update(executeSql.f1.get(1).f0,executeSql.f1.get(1).f1.toArray());
                        }
                    }
                }catch (Exception e){
                    LOGGER.error("执行语句：{}", JSONObject.toJSONString(value));
                    LOGGER.error("执行失败：",e);
                    // 事务回滚
                    status.setRollbackOnly();
                }
            }
        });
    }
}
