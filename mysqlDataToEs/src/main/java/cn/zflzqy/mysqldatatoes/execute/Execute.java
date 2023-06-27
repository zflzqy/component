package cn.zflzqy.mysqldatatoes.execute;

import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.handler.HandlerService;
import com.alibaba.fastjson.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author: zfl
 * @Date: 2023-05-30-19:40
 * @Description: 数据处理执行类
 */
public class Execute {
    // 增量处理器
    private static LinkedList<HandlerService> excueteHandlerServices = new LinkedList<>();
    // 全量处理器
    private static LinkedList<HandlerService> fullHandlerServices = new LinkedList<>();
    private HandlerEnum handlerEnum;

    public HandlerEnum getHandlerEnum() {
        return handlerEnum;
    }

    public Execute(HandlerEnum handlerEnum) {
        this.handlerEnum = handlerEnum;
    }

    public void execute(JSONObject jsonObject,Class asClass){
        List<HandlerService> handlerServices = null;
        if (handlerEnum==HandlerEnum.INCREMENTAL){
            handlerServices = excueteHandlerServices;
        }else if (handlerEnum==HandlerEnum.FULL){
            handlerServices = fullHandlerServices;
        }
        for (HandlerService handlerService : handlerServices){
            handlerService.execute(jsonObject,asClass);
        }
    }

    public static LinkedList<HandlerService> getExcueteHandlerServices() {
        return excueteHandlerServices;
    }

    public static void setHandlerServices(LinkedList<HandlerService> handlerServices) {
        Execute.excueteHandlerServices = handlerServices;
    }

    public static LinkedList<HandlerService> getFullHandlerServices() {
        return fullHandlerServices;
    }

    public static void setFullHandlerServices(LinkedList<HandlerService> fullHandlerServices) {
        Execute.fullHandlerServices = fullHandlerServices;
    }
}
