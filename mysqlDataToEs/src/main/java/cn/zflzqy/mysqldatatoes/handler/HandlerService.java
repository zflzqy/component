package cn.zflzqy.mysqldatatoes.handler;

import cn.zflzqy.mysqldatatoes.enums.HandlerEnum;
import cn.zflzqy.mysqldatatoes.execute.Execute;
import com.alibaba.fastjson.JSONObject;

/**
 * @Author: zfl
 * @Date: 2023-05-30-19:44
 * @Description:
 */
public interface HandlerService {
    public static void register(HandlerEnum handlerEnum,HandlerService handlerService){
        if (handlerEnum== HandlerEnum.INCREMENTAL) {
            Execute.getExcueteHandlerServices().add(handlerService);
        }else if (handlerEnum== HandlerEnum.FULL){
            Execute.getFullHandlerServices().add(handlerService);
        }
    }
    public void execute(JSONObject jsonObject,Class aclass);
}
