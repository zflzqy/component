package cn.zflzqy.mysqldatatoes.execute;

import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import cn.zflzqy.mysqldatatoes.handler.TransDateHandler;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;

import java.util.LinkedList;

/**
 * @Author: zfl
 * @Date: 2023-05-30-19:40
 * @Description: 数据处理执行类
 */
public class Execute {
    private static LinkedList<HandlerService> handlerServices = new LinkedList<>();
    static {
        HandlerService.register(new TransDateHandler());
    }
    public Execute() {}

    public void execute(JSONObject jsonObject,Class asClass){
        for (HandlerService handlerService : handlerServices){
            handlerService.execute(jsonObject,asClass);
        }
    }

    public static LinkedList<HandlerService> getHandlerServices() {
        return handlerServices;
    }

    public static void setHandlerServices(LinkedList<HandlerService> handlerServices) {
        Execute.handlerServices = handlerServices;
    }
}
