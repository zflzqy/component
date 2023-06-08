package cn.zflzqy.mysqldatatoes;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ElasticsearchQuery {

    public static void main(String[] args) {
        // 创建 RestHighLevelClient 客户端
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("192.168.19.129", 9200)));

        try {
            // 构建查询请求
            SearchRequest searchRequest = new SearchRequest( "t_base");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.multiMatchQuery("谷物", "*"));

            // 添加高亮查询
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("*"); // 设置要高亮显示的字段
            highlightBuilder.preTags("<span style='color:red'>"); // 设置高亮显示的前缀
            highlightBuilder.postTags("</span>"); // 设置高亮显示的后缀
            sourceBuilder.highlighter(highlightBuilder);

            searchRequest.source(sourceBuilder);

            // 执行查询请求
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            // 处理查询结果
            SearchHit[] hits = searchResponse.getHits().getHits();
            List<Map<String, Object>> results = new ArrayList<>();
            for (SearchHit hit : hits) {
                // 获取文档的原始数据
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();

                // 获取高亮字段的内容
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                Iterator<Map.Entry<String, HighlightField>> iterator = highlightFields.entrySet().iterator();
                while (iterator.hasNext()) {
                    // 反写到原始数据种
                    Map.Entry<String, HighlightField> next = iterator.next();
                    String key = next.getKey();
                    HighlightField value = next.getValue();
                    String rs = value.getFragments()[0].toString();
                    sourceAsMap.put(key,rs);

                }
                results.add(sourceAsMap);
            }
            System.out.println("jieg "+JSONObject.toJSONString(results));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭客户端连接
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
