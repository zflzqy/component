package cn.zflzqy.mysqldatatoes.consumer;

import cn.zflzqy.mysqldatatoes.annotation.RequestUrl;
import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.debezium.engine.ChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description 数据消费
 */

public class CustomConsumer implements Consumer<ChangeEvent<String, String>> {
    private static final Logger log = LoggerFactory.getLogger(CustomConsumer.class);
    /** 索引类数据*/
    private Map<String, Class> indexs;
    /** 数据处理执行器*/
    private Execute execute;
    /** es操作模板*/
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public CustomConsumer(Map<String, Class> indexs, Execute execute,ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.indexs = indexs;
        this.execute = execute;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    @Override
    public void accept(ChangeEvent<String, String> record) {
        if (record == null || !StringUtils.hasText(record.value())) {
            log.warn("数据没有value值");
            return;
        }

        // 处理的数据
        String value = record.value();
        try {
            // 将字符串转换为 JsonObject
            JSONObject jsonObject = JSONObject.parseObject(value);
            JSONObject payload = jsonObject.getJSONObject("payload");
            JSONObject source = payload.getJSONObject("source");
            String table = source.getString("table");
            if (!indexs.containsKey(table)) {
                log.info("未配置{}的索引实体，跳过", table);
                return;
            }

            // 检查是否存在索引
            boolean exists = elasticsearchRestTemplate.indexOps(indexs.get(table)).exists();
            if (!exists) {
                elasticsearchRestTemplate.indexOps(indexs.get(table)).createWithMapping();
            }

            // 处理数据
            execute.execute(jsonObject, indexs.get(table));

            // 获取操作类型
            OpEnum opEnum = null;
            try {
                opEnum = OpEnum.valueOf(payload.getString("op"));
            } catch (Exception e) {
                log.warn("无相关枚举:{}", payload.getString("op"));
                return;
            }
            switch (opEnum) {
                case r:
                case c:
                case u:
                    addEsData(elasticsearchRestTemplate, indexs, payload.getJSONObject("after"), table, opEnum);
                    break;
                case d:
                    addEsData(elasticsearchRestTemplate, indexs, payload.getJSONObject("before"), table, opEnum);
                default:
            }
        } catch (Exception e) {
            log.error("处理异常", e);
        }
    }




    /**
     * @description 添加数据到es
     * @param elasticsearchRestTemplate
     * @param indexs
     * @param data
     * @param table
     * @param opEnum
     */
    public static void addEsData(ElasticsearchRestTemplate elasticsearchRestTemplate,
                                 Map<String, Class> indexs, JSONObject data, String table, OpEnum opEnum) {
        List<JSONObject> datas = new ArrayList<JSONObject>();
        datas.add(data);
        addEsData(elasticsearchRestTemplate, indexs,datas,table,opEnum);

    }

    /**
     * @description 添加数据到es
     * @param elasticsearchRestTemplate
     * @param indexs
     * @param data
     * @param table
     * @param opEnum
     */
    public static void addEsData(ElasticsearchRestTemplate elasticsearchRestTemplate,
                                 Map<String, Class> indexs, List<JSONObject> data, String table, OpEnum opEnum) {
        Class aClass = indexs.get(table);
        // 构建es索引
        IndexCoordinates indexCoordinates = elasticsearchRestTemplate.getIndexCoordinatesFor(aClass);
        // 获取id字段
        String idPropertyName = null;
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(Id.class)!=null) {
                idPropertyName = field.getName();
            }
        }

        // 得到前端跳转的请求参数地址
        String requestUrl = null;
        Annotation annotation = aClass.getAnnotation(RequestUrl.class);
        if (annotation!=null) {
            requestUrl = ((RequestUrl) annotation).requestUrl();
        }

        // 根据不同的crud类型返回不同的数据
        switch (opEnum) {
            case r:
            case c:
                List<IndexQuery> indexQueries = new ArrayList<IndexQuery>();
                for (int i = 0; i <data.size(); i++) {
                    data.get(i).put("requestUrl",buildRequestUrl(requestUrl,data.get(i)));
                    IndexQuery indexQuery = new IndexQuery();
                    indexQuery.setId(data.get(i).getString(idPropertyName));
                    indexQuery.setSource(data.get(i).toString(SerializerFeature.WriteDateUseDateFormat,SerializerFeature.WriteMapNullValue));
                    indexQueries.add(indexQuery);
                }
                elasticsearchRestTemplate.bulkIndex(indexQueries,indexCoordinates);
                break;
            case u:
                List<UpdateQuery> updateQueries = new ArrayList<UpdateQuery>();
                for (int i = 0; i <data.size(); i++) {
                    data.get(i).put("requestUrl",buildRequestUrl(requestUrl,data.get(i)));
                    UpdateQuery updateQuery = UpdateQuery.builder(data.get(i).getString(idPropertyName))
                            .withDocAsUpsert(true)
                            .withDocument(Document.parse(data.get(i).toString(SerializerFeature.WriteDateUseDateFormat,SerializerFeature.WriteMapNullValue)))
                            .build();
                    updateQueries.add(updateQuery);
                }
                elasticsearchRestTemplate.bulkUpdate(updateQueries,indexCoordinates);
                break;
            case d:
                for (int i = 0; i <data.size(); i++) {
                    elasticsearchRestTemplate.delete(JSONObject.toJavaObject(data.get(i),aClass));
                }
            default:
        }
    }


    /**
     * 替换请求中的参数信息
     * @param content
     * @param data
     * @return
     */
    private static String buildRequestUrl(String content, JSONObject data){
        if (!StringUtils.hasText(content)){
            return "";
        }
        String pattern = "\\{(.*?)\\}".intern();
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (m.find())
        {
            String key = m.group(1);
            String value = data.getString(key);
            m.appendReplacement(sb, value == null ? "" : value);
        }
        m.appendTail(sb);
        return  sb.toString();
    }
}