package cn.zflzqy.mysqldatatoes.execute;

import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.event.entity.SyncDatatExcuteEvent;
import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.thread.SyncThread;
import cn.zflzqy.mysqldatatoes.thread.ThreadPoolFactory;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import cn.zflzqy.mysqldatatoes.util.JedisPoolUtil;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author zfl
 */
public class SyncDataExecute {
    private static final Logger logger = LoggerFactory.getLogger(SyncDataExecute.class);

    // 执行的进度rediskey
    public static final String SYNC_DATA_HANDLER = "esToMysqlData::sync::data:handler";

    private static final int REDIS_EXPIRE = 604800;

    @Autowired
    private MysqlDataToEsPropertites properties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private final Execute execute;

    public SyncDataExecute() {
        execute = new Execute(HandlerEnum.FULL);

    }

    /**
     * 支持表级过滤
     */
    @Async
    public void process() throws InterruptedException {
        process(null);
    }

    @Async
    public void process(Set<String> tables) throws InterruptedException {
        // 开始时间
        long startTime = System.currentTimeMillis();
        JedisPool jedisPool = JedisPoolUtil.getInstance();

        // 创建数据库连接池
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(properties.getMysqlUrl());
        dataSource.setUsername(properties.getMysqlUsername());
        dataSource.setPassword(properties.getMysqlPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 查询所有的表数据
        Map<String, Class> indexs = PackageScan.getIndexes();
        if (CollectionUtils.isEmpty(tables)) {
            tables = indexs.keySet();
        } else {
            for (String table : tables) {
                if (!indexs.containsKey(table)) {
                    tables.remove(table);
                }
            }
        }
        JSONArray offset = new JSONArray();

        // mysql连接信息
        JdbcUrlParser.JdbcConnectionInfo jdbcConnectionInfo = JdbcUrlParser.parseJdbcUrl(properties.getMysqlUrl());
        // rediskey
        String redisKey = SYNC_DATA_HANDLER + "::" + jdbcConnectionInfo.getHost()
                + "::" + jdbcConnectionInfo.getPort()
                + "::" + jdbcConnectionInfo.getDatabase()
                + "::" + properties.getMysqlUsername();

        // cpu核心数据
        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolFactory.ThreadPoolFactoryBuilderImpl()
                .corePoolSize(8)
                .maximumPoolSize(2*cpuCoreNum+1)
                .keepAliveTime(30L)
                .prefix("mysql-data-to-es-all")
                .build();

        // 批次查询上限
        int batchSize = 5000;
        for (String table : tables) {
            JSONObject tableExecute = new JSONObject();
            offset.add(tableExecute);
            int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
            // 先删除索引，主要是为了防止部分数据残留
            elasticsearchRestTemplate.indexOps(indexs.get(table)).delete();
            // 创建索引
            elasticsearchRestTemplate.indexOps(indexs.get(table)).createWithMapping();
            tableExecute.put("table", table);
            tableExecute.put("count", total);
            tableExecute.put("batchSize", batchSize);

            // 添加偏移
            addOffset(jedisPool, offset, redisKey);
            if (total <= 0) {
                // 继续下个表
                continue;
            }

            // 然后，我们使用一个循环来按批次查询
            for (int i = 0; i < total; i += batchSize) {
                // 我们使用LIMIT和OFFSET关键字来按批次查询
                List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM " + table + " LIMIT ? OFFSET ?", batchSize, i);
                if (CollectionUtils.isEmpty(result)) {
                    continue;
                }

                // 将数据异步出去
                int pageSize = 100;
                int page = (int) Math.ceil((double) result.size() / pageSize);
                CountDownLatch countDownLatch = new CountDownLatch(page);
                for (int j=0;j<page;j++) {
                    int end = j+1==page?result.size():(j + 1) * pageSize;
                    List<Map<String, Object>> subList = result.subList(j * pageSize, end);
                    threadPoolExecutor.submit(new SyncThread(subList, execute, indexs, table, elasticsearchRestTemplate,countDownLatch));
                }

                // 阻塞等待线程池任务执行完
                countDownLatch.await();
                // 添加偏移
                tableExecute.put("current", total);
                addOffset(jedisPool, offset, redisKey);
            }
        }
        dataSource = null;
        // 线程池释放
        threadPoolExecutor.shutdown();
        // 结束时间
        long endTime = System.currentTimeMillis();
        logger.info("全量同步任务"+tables.toString()+"耗时："+(endTime-startTime));

    }

    /**
     * 添加偏移记录
     *
     * @param offset：偏移数据
     */
    private void addOffset(JedisPool jedisPool, JSONArray offset, String redisKey) {
        // 写入redis进度
        try (Jedis jedis = jedisPool.getResource()) {
            // 在这里使用jedis实例来操作Redis
            jedis.setex(redisKey, REDIS_EXPIRE, offset.toString());
        }

        // 发布事件
        eventPublisher.publishEvent(new SyncDatatExcuteEvent(offset.toString()));
    }


}
