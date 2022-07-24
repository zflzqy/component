package cn.zflzqy.readMysqlBinlog.sink;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.pool.DruidPool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @Author: zfl
 * @Date: 2022-07-23-10:57
 * @Description:
 */
public class JdbcTemplateSink<IN> extends RichSinkFunction<IN> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTemplateSink.class);
    private JdbcTemplate jdbcTemplate;
    private DataBase dataBase;

    public JdbcTemplateSink(DataBase dataBase) {
        this.dataBase = dataBase;
    }

    public void open(Configuration parameters) throws Exception {
        jdbcTemplate = new JdbcTemplate();
        DataSource dataSource = DruidPool.getDataSource(dataBase);
        jdbcTemplate.setDataSource(dataSource);
        LOGGER.info("获取数据库连接池：{},构建jdbcTemplate:{}",dataSource.hashCode(),jdbcTemplate.hashCode());
        super.open(parameters);
    }
    @Override
    public void invoke(IN value, Context context) throws Exception {
        // 执行方法
        LOGGER.info(value.toString());
        super.invoke(value, context);
    }
}
