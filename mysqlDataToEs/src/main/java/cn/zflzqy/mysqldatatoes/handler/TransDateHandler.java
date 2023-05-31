package cn.zflzqy.mysqldatatoes.handler;

import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.client.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * @Author: zfl
 * @Date: 2023-05-30-19:39
 * @Description: 处理事件
 */
public class TransDateHandler implements HandlerService {

    @Override
    public void execute(JsonObject jsonObject) {
        JsonObject payload = jsonObject.getAsJsonObject("payload");
        JsonObject source = payload.getAsJsonObject("source");
        if (!jsonObject.has("schema")){
            return;
        }
        JsonObject schema = jsonObject.getAsJsonObject("schema");
        if (!schema.has("fields")){
            return;
        }
        JsonArray fields = schema.getAsJsonArray("fields");
        if (!payload.has("op")){
            return;
        }

        OpEnum opEnum = OpEnum.valueOf(payload.get("op").getAsString());
        // 根据不同的crud类型返回不同的数据
        switch (opEnum){
            case r:
                for (int i = 0; i < fields.size(); i++){
                    JsonObject asJsonObject = fields.get(i).getAsJsonObject();
                    if (asJsonObject.get("field").getAsString().equals("after")){
                        deal(payload.getAsJsonObject("after"),asJsonObject.getAsJsonArray("fields"));
                    }
                }
                break;
            case c:
                for (int i = 0; i < fields.size(); i++){
                    JsonObject asJsonObject = fields.get(i).getAsJsonObject();
                    if (asJsonObject.get("field").getAsString().equals("after")){
                        deal(payload.getAsJsonObject("after"),asJsonObject.getAsJsonArray("fields"));
                    }
                }
                break;
            case u:
                for (int i = 0; i < fields.size(); i++){
                    JsonObject asJsonObject = fields.get(i).getAsJsonObject();
                    if (asJsonObject.get("field").getAsString().equals("after")){
                        deal(payload.getAsJsonObject("after"),asJsonObject.getAsJsonArray("fields"));
                    }
                }
                break;
            case d:
                for (int i = 0; i < fields.size(); i++){
                    JsonObject asJsonObject = fields.get(i).getAsJsonObject();
                    if (asJsonObject.get("field").getAsString().equals("before")){
                        deal(payload.getAsJsonObject("before"),asJsonObject.getAsJsonArray("fields"));
                    }
                }
//                elasticsearchRestTemplate.delete(gson.fromJson(payload.get("after").getAsJsonObject().toString(), indexs.get(table)));
            default:
        }
    }
    public void deal(JsonObject data,JsonArray fields){
        for (int i = 0; i < fields.size(); i++){
            JsonObject asJsonObject = fields.get(i).getAsJsonObject();
            if (asJsonObject.has("name")) {
                // 获取字段名称，根据日期类型进行不同的处理
                String name = asJsonObject.get("name").getAsString();
                String field = asJsonObject.get("field").getAsString();
                // todo 转换时间

                if (name.equals("io.debezium.time.Date")){
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    LocalDate epoch = LocalDate.of(1970, 1, 1);
                    LocalDate today = epoch.plusDays(data.get(field).getAsInt());
                    // 将 LocalDate 转换为 Date
                    Date date = java.sql.Date.valueOf(today);
                    data.addProperty(field, dateFormat.format(date));
                }else if (name.equals("io.debezium.time.MicroTime")){

                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                    Date date = new Date(data.get(field).getAsLong()/1000);
                    // 创建一个 Calendar 对象，并设置时间为 Date 对象的值
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    // 将 Calendar 对象的时间向后偏移8小时
                    calendar.add(Calendar.HOUR_OF_DAY, -8);
                    // 获取偏移后的时间
                    Date offsetDate = calendar.getTime();
                    data.addProperty(field, dateFormat.format(offsetDate));

                }else if (name.equals("io.debezium.time.Timestamp")){

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date(data.get(field).getAsLong());
                    // 创建一个 Calendar 对象，并设置时间为 Date 对象的值
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    // 将 Calendar 对象的时间向后偏移8小时
                    calendar.add(Calendar.HOUR_OF_DAY, -8);
                    // 获取偏移后的时间
                    Date offsetDate = calendar.getTime();


                    data.addProperty(field, dateFormat.format(offsetDate));

                }else if (name.equals("io.debezium.time.MicroTimestamp")) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Date date = new Date(data.get(field).getAsLong()/1000);
                    // 创建一个 Calendar 对象，并设置时间为 Date 对象的值
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    // 将 Calendar 对象的时间向后偏移8小时
                    calendar.add(Calendar.HOUR_OF_DAY, -8);
                    // 获取偏移后的时间
                    Date offsetDate = calendar.getTime();
                    data.addProperty(field, dateFormat.format(offsetDate));
                }

            }

        }

    }
}
