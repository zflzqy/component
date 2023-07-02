package cn.zflzqy.mysqldatatoes.execute;

import cn.zflzqy.mysqldatatoes.config.CustomConsumer;
import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import cn.zflzqy.mysqldatatoes.event.entity.SyncDatatExcuteEvent;
import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import cn.zflzqy.mysqldatatoes.util.JdbcUrlParser;
import cn.zflzqy.mysqldatatoes.util.JedisPoolUtil;
import cn.zflzqy.mysqldatatoes.util.PackageScan;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncDatatExcute {

    // 执行的进度rediskey
    public static final String SYNC_DATA_HANDLER = "esToMysqlData::sync::data:handler";

    private static final int REDIS_EXPIRE = 604800;

    @Autowired
    private MysqlDataToEsPropertites properties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Async
    public void process() {
        JedisPool jedisPool = JedisPoolUtil.getInstance();

        // 创建数据库连接池
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(properties.getMysqlUrl());
        dataSource.setUsername(properties.getMysqlUsername());
        dataSource.setPassword(properties.getMysqlPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 查询所有的表数据
        Map<String, Class> indexs = PackageScan.getIndexs();
        Set<String> tables = indexs.keySet();
        JSONArray offset = new JSONArray();

        // mysql连接信息
        JdbcUrlParser.JdbcConnectionInfo jdbcConnectionInfo = JdbcUrlParser.parseJdbcUrl(properties.getMysqlUrl());
        // rediskey
        String redisKey = SYNC_DATA_HANDLER+"::"+jdbcConnectionInfo.getHost()
                +"::"+jdbcConnectionInfo.getPort()
                +"::"+jdbcConnectionInfo.getDatabase()
                +"::"+properties.getMysqlUsername();

        // 数据处理执行类
        Execute execute = new Execute(HandlerEnum.FULL);

        // 批次查询上限
        int batchSize = 5000;
        for (String table : tables) {
            JSONObject tableExecute = new JSONObject();
            offset.add(tableExecute);
            int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM "+table, Integer.class);
            // 先删除索引，主要是为了防止部分数据残留
            elasticsearchRestTemplate.indexOps(indexs.get(table)).delete();
            // 创建索引
            elasticsearchRestTemplate.indexOps(indexs.get(table)).create();
            tableExecute.put("table",table);
            tableExecute.put("count",total);
            tableExecute.put("batchSize",batchSize);

            // 添加偏移
            addOffset(jedisPool,offset,redisKey);
            if (total<=0){
                // 继续下个表
                continue;
            }

            // 然后，我们使用一个循环来按批次查询
            for (int i = 0; i < total; i += batchSize) {
                // 我们使用LIMIT和OFFSET关键字来按批次查询
                List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT * FROM "+table+" LIMIT ? OFFSET ?", batchSize, i);

                // 将数据写入到es中
                JSONArray datas = new JSONArray();
                for (Map<String, Object> entry : result){
                    JSONObject jsonObject = new JSONObject(entry);
                    execute.execute(jsonObject,indexs.get(table));
                    datas.add(jsonObject);
                }
                CustomConsumer.addEsData(elasticsearchRestTemplate, indexs,datas, table, OpEnum.r);

                // 添加偏移
                tableExecute.put("current",total);
                addOffset(jedisPool,offset,redisKey);

            }

        }
        jedisPool.close();
        dataSource = null;
    }

    /**
     * 添加偏移记录
     * @param offset：偏移数据
     */
    private void addOffset(JedisPool jedisPool, JSONArray offset,String redisKey) {
        // 写入redis进度
        try (Jedis jedis = jedisPool.getResource()) {
            // 在这里使用jedis实例来操作Redis
            jedis.setex(redisKey,REDIS_EXPIRE, offset.toString());
        }

        // 发布事件
        eventPublisher.publishEvent(new SyncDatatExcuteEvent(offset.toString()));
    }


}
