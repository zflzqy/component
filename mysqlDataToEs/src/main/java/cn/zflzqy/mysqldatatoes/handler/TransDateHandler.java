package cn.zflzqy.mysqldatatoes.handler;

import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;


/**
 * @Author: zfl
 * @Date: 2023-05-30-19:39
 * @Description: 处理事件
 */
public class TransDateHandler implements HandlerService {

    @Override
    public void execute(JSONObject jsonObject,Class asClass)  {
        JSONObject payload = jsonObject.getJSONObject("payload");
        JSONObject source = payload.getJSONObject("source");
        if (!jsonObject.containsKey("schema")){
            return;
        }
        JSONObject schema = jsonObject.getJSONObject("schema");
        if (!schema.containsKey("fields")){
            return;
        }
        JSONArray fields = schema.getJSONArray("fields");
        if (!payload.containsKey("op")){
            return;
        }

        OpEnum opEnum = OpEnum.valueOf(payload.getString("op"));
        // 根据不同的crud类型返回不同的数据
        switch (opEnum){
            case r:
            case c:
            case u:
                for (int i = 0; i < fields.size(); i++){
                    JSONObject asJsonObject = fields.getJSONObject(i);
                    if ("after".equals(asJsonObject.getString("field"))){
                        deal(payload.getJSONObject("after"),asJsonObject.getJSONArray("fields"));
                    }
                }
                break;
            case d:
                for (int i = 0; i < fields.size(); i++){
                    JSONObject asJsonObject = fields.getJSONObject(i);
                    if ("before".equals(asJsonObject.getString("field"))){
                        deal(payload.getJSONObject("before"),asJsonObject.getJSONArray("fields"));
                    }
                }
//                elasticsearchRestTemplate.delete(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
            default:
        }
    }
    public void deal(JSONObject data,JSONArray fields){
        for (int i = 0; i < fields.size(); i++){
            JSONObject asJsonObject = fields.getJSONObject(i);
            if (asJsonObject.containsKey("name")) {
                // 获取字段名称，根据日期类型进行不同的处理
                String name = asJsonObject.getString("name");
                String field = asJsonObject.getString("field");
                if (!StringUtils.hasText(field)|| ObjectUtils.isEmpty(data.get(field))){
                    continue;
                }

                if (name.equals("io.debezium.time.Date")){
                    LocalDate epoch = LocalDate.of(1970, 1, 1);
                    LocalDate today = epoch.plusDays(data.getInteger(field));
                    // 将 LocalDate 转换为 Date
                    Date date = java.sql.Date.valueOf(today);
                    data.put(field, date);
                }else if (name.equals("io.debezium.time.MicroTime")){
                    Date date = new Date(data.getLong(field)/1000);
                    data.put(field, date);
                }else if (name.equals("io.debezium.time.Timestamp")){
                    Date date = new Date(data.getLong(field));
                    data.put(field, date);

                }else if (name.equals("io.debezium.time.MicroTimestamp")) {
                    Date date = new Date(data.getLong(field)/1000);
                    data.put(field, date);
                }

            }

        }

    }
}
