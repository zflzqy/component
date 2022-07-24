package cn.zflzqy.readMysqlBinlog.pool;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import com.alibaba.druid.pool.DruidDataSource;
import org.apache.flink.table.expressions.In;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public class DruidPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(DruidPool.class);
    private static ConcurrentHashMap<Integer, DataSource> dataSources = new ConcurrentHashMap<>(6);
    private static boolean ex;

    // 静态代码块
    public synchronized static void create(DataBase dataBase) {
        if (null != dataSources.get(dataBase.hashCode())) {
            return;
        }
        LOGGER.info("初始化前连接池大小：{}", dataSources.size());
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://" + dataBase.getIp() + ":" + dataBase.getPort() + "/" + dataBase.getDatabaseName() + "?useUnicode=true&useSSL=true&autoReconnect=true&failOverReadOnly=false&serverTimezone=Asia/Shanghai");
        druidDataSource.setUsername(dataBase.getUsername());
        druidDataSource.setPassword(dataBase.getPassword());
        // 初始化时建立物理连接的个数。初始化发生在显示调用init方法，或者第一次getConnection时
        druidDataSource.setInitialSize(10);
        // 最小连接池数量
        druidDataSource.setMinIdle(10);
        // 最大连接池数量
        druidDataSource.setMaxActive(20);
        // 获取连接时最大等待时间，单位毫秒。配置了maxWait之后，缺省启用公平锁，并发效率会有所下降，如果需要可以通过配置useUnfairLock属性为true使用非公平锁。
        druidDataSource.setMaxWait(1000 * 20);
        // 有两个含义：1) Destroy线程会检测连接的间隔时间2) testWhileIdle的判断依据，详细看testWhileIdle属性的说明
        druidDataSource.setTimeBetweenEvictionRunsMillis(1000 * 60);
        // <!-- 配置一个连接在池中最大生存的时间，单位是毫秒 -->
        druidDataSource.setMaxEvictableIdleTimeMillis(1000 * 60 * 60 * 10);
        // <!-- 配置一个连接在池中最小生存的时间，单位是毫秒 -->
        druidDataSource.setMinEvictableIdleTimeMillis(1000 * 60 * 60 * 9);
        // <!-- 这里建议配置为TRUE，防止取到的连接不可用 -->
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setTestOnBorrow(true);
        druidDataSource.setTestOnReturn(false);
        druidDataSource.setValidationQuery("select 1");
        druidDataSource.setAsyncInit(true);
        try {
            LOGGER.info(dataBase.getClass().getName());
            dataSources.put(dataBase.hashCode(), druidDataSource);
            LOGGER.info("连接池初始化成功:{},连接池大小：{}", druidDataSource.hashCode(), dataSources.size());
        } catch (Exception e) {
            LOGGER.info("初始化连接池异常：{}", e.getMessage());
        }
    }

    private DruidPool() {
    }

    public static DataSource getDataSource(DataBase dataBase) throws InterruptedException {
        DataSource dataSource = dataSources.get(dataBase.hashCode());
        if (null == dataSource) {
            create(dataBase);
        }
        return dataSources.get(dataBase.hashCode());
    }
}

