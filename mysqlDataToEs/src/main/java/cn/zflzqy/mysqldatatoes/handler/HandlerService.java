package cn.zflzqy.mysqldatatoes.handler;

import cn.zflzqy.mysqldatatoes.execute.Execute;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @Author: zfl
 * @Date: 2023-05-30-19:44
 * @Description:
 */
public interface HandlerService {
    public static void register(HandlerService handlerService){
        Execute.getHandlerServices().add(handlerService);
    }
    public void execute(JSONObject jsonObject);
}
