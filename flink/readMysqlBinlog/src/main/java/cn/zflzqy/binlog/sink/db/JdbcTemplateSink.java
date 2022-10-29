package cn.zflzqy.binlog.sink.db;

import cn.zflzqy.binlog.model.db.DataBase;
import cn.zflzqy.binlog.common.pool.DruidPool;
import cn.zflzqy.binlog.transform.enums.OpEnum;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
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
import java.util.*;

/**
 * @Author: zfl
 * @Date: 2022-07-23-10:57
 * @Description:
 */
public class JdbcTemplateSink<IN> extends RichSinkFunction<IN> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTemplateSink.class);
    /**
     * 数据库信息
     */
    private List<DataBase> dataBases = new ArrayList<>();
    /**
     * jdbc组件
     */
    private List<JdbcTemplate> jdbcTemplates = new ArrayList<>();
    /**
     * jdbc事务组件
     */
    private List<TransactionTemplate> transactionTemplates = new ArrayList<>();

    public JdbcTemplateSink(DataBase dataBase) {
        // 初始化数据库信息
        this.init(dataBase);
    }

    public JdbcTemplateSink() {
    }

    /**
     * 初始化连接池信息
     *
     * @param dataBase
     */
    private synchronized void init(DataBase dataBase) {
        LOGGER.info("需要处理的数据源信息：{}", JSONObject.toJSONString(dataBase));
        // 需要判断这个db是否在了
        boolean exists = false;
        for (DataBase db : dataBases) {
            if (dataBase.hashCode() == db.hashCode()) {
                exists =true;
                break;
            }
        }
        if (!exists){
            // 创建连接池和事务操作
            LOGGER.info("创建数jdbc:{},{},{}", dataBase.getIp(), dataBase.getPort(), dataBase.getDatabaseName());
            // 获取or创建连接池
            DataSource druidDataSource = DruidPool.getDataSource(dataBase);
            // jdbc
            JdbcTemplate jdbcTemplate = new JdbcTemplate();
            jdbcTemplate.setDataSource(druidDataSource);
            this.jdbcTemplates.add(jdbcTemplate);
            // jdbc事务
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(druidDataSource);
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            this.transactionTemplates.add(transactionTemplate);
            LOGGER.info("获取数据库连接池：{},构建jdbcTemplate:{},构建transactionTemplate：{}", druidDataSource.hashCode(), jdbcTemplate.hashCode(), transactionTemplate.hashCode());
            this.dataBases.add(dataBase);
        }

    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }

    /**
     * 获取jdbcTemplate
     *
     * @param dataBase: 数据库信息
     * @return
     */
    public JdbcTemplate getJdbcTemplate(DataBase dataBase) {
        if (null == dataBase) {
            LOGGER.warn("未找到对应数据库的连接池，数据库信息为空");
            return null;
        }
        // 初始化
        this.init(dataBase);
        // 获取jdbc信息
        for (int i = 0; i < dataBases.size(); i++) {
            if (dataBases.get(i).hashCode() == dataBase.hashCode()) {
                return this.jdbcTemplates.get(i);
            }
        }
        LOGGER.warn("未找到对应数据库的连接池，数据库信息为空,jdbcTemplate池");
        return null;
    }

    /**
     * 获取TransactionTemplate
     *
     * @param dataBase: 数据库信息
     * @return
     */
    public TransactionTemplate getTxJdbcTemplate(DataBase dataBase) {
        if (null == dataBase) {
            LOGGER.warn("未找到对应数据库的连接池，数据库信息为空");
            return null;
        }
        // 初始化
        this.init(dataBase);
        // 获取jdbc信息
        for (int i = 0; i < dataBases.size(); i++) {
            if (dataBases.get(i).hashCode() == dataBase.hashCode()) {
                return this.transactionTemplates.get(i);
            }
        }
        LOGGER.warn("未找到对应数据库的连接池，数据库信息为空,transactionTemplate池");
        return null;
    }

    @Override
    public void invoke(IN value, Context context) throws Exception {
        super.invoke(value, context);
        Tuple2<DataBase, Object> out = (Tuple2<DataBase, Object>) value;
        // 获取jdbcTemplate
        JdbcTemplate jdbcTemplate = getJdbcTemplate(out.f0);
        // 获取事务级的jdbcTemplate
        TransactionTemplate transactionTemplate = getTxJdbcTemplate(out.f0);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {

                Tuple2<String, List<Tuple2<String, List<Object>>>> executeSql = (Tuple2<String, List<Tuple2<String, List<Object>>>>) out.f1;
                try {
                    // 如果是删除或更新
                    if (StringUtils.equals(executeSql.f0, OpEnum.d.name()) || StringUtils.equals(executeSql.f0, OpEnum.u.name())) {
                        for (int i = 0; i < executeSql.f1.size(); i++) {
                            jdbcTemplate.update(executeSql.f1.get(i).f0, executeSql.f1.get(i).f1.toArray());
                        }
                    } else {
                        // 其他情况，就是快照读和新增，需要先查询一次数据
                        List<Map<String, Object>> list = jdbcTemplate.queryForList(executeSql.f1.get(0).f0, executeSql.f1.get(0).f1.toArray());
                        if (CollectionUtils.isEmpty(list)) {
                            // 从数组第二个获取插入语句
                            jdbcTemplate.update(executeSql.f1.get(1).f0, executeSql.f1.get(1).f1.toArray());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("执行语句：{}", JSONObject.toJSONString(value));
                    LOGGER.error("执行失败：", e);
                    // 事务回滚
                    status.setRollbackOnly();
                }
            }
        });
    }
}
