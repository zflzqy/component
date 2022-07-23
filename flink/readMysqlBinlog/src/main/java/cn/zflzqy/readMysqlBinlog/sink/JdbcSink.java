package cn.zflzqy.readMysqlBinlog.sink;

import cn.zflzqy.readMysqlBinlog.DruidConnectionPool;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-23-10:57
 * @Description:
 */
public class JdbcSink<IN> extends RichSinkFunction<IN> {
    private JdbcTemplate jdbcTemplate;

    public void open(Configuration parameters) throws Exception {
        jdbcTemplate = new JdbcTemplate();
        DataSource connection = DruidConnectionPool.getConnection();
        System.out.println("数据连接池1："+connection.hashCode());
        jdbcTemplate.setDataSource(connection);
        super.open(parameters);
    }
    @Override
    public void invoke(IN value, Context context) throws Exception {
        System.out.println(this.jdbcTemplate);
        List<Map<String, Object>> maps = jdbcTemplate.queryForList("select  * from  resource");
        System.out.println(maps.size());
        System.out.println(this.jdbcTemplate.hashCode());
        super.invoke(value, context);
    }
}
