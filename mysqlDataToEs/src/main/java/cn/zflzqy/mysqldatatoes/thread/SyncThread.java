package cn.zflzqy.mysqldatatoes.thread;

import cn.zflzqy.mysqldatatoes.config.CustomConsumer;
import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2023-07-11-21:11
 * @Description:
 */
public class SyncThread implements Runnable {
    private List<Map<String,Object>> result;
    private Execute execute;
    private Map<String,Class> indexs;
    private String table;
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public SyncThread(List<Map<String, Object>> result, Execute execute, Map<String, Class> indexs, String table, ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.result = result;
        this.execute = execute;
        this.indexs = indexs;
        this.table = table;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    @Override
    public void run() {
        // 将数据写入到es
        JSONArray datas = new JSONArray();
        for (Map<String, Object> entry : result) {
            JSONObject jsonObject = new JSONObject(entry);
            execute.execute(jsonObject, indexs.get(table));
            datas.add(jsonObject);
        }
        CustomConsumer.addEsData(elasticsearchRestTemplate, indexs, datas, table, OpEnum.r);
    }
}
