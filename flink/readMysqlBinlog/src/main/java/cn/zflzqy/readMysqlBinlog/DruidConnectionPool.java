package cn.zflzqy.readMysqlBinlog;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

public class DruidConnectionPool {

    private transient static DataSource dataSource = null;
    private transient static Properties props = new Properties();

    // 静态代码块
    static {
        props.put("driverClassName", "com.mysql.jdbc.Driver");
        props.put("url", "jdbc:mysql://192.168.50.102:3306/myapp?useUnicode=true&useSSL=true&autoReconnect=true&failOverReadOnly=false&serverTimezone=Asia/Shanghai");
        props.put("username", "root");
        props.put("password", "123456");
        try {
            dataSource = DruidDataSourceFactory.createDataSource(props);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DruidConnectionPool() {
    }


    public static DataSource getConnection() throws SQLException {
        return dataSource;
    }
}

