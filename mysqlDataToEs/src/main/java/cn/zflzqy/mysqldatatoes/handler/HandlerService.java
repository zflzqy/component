package cn.zflzqy.mysqldatatoes.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @Author: zfl
 * @Date: 2023-05-30-19:44
 * @Description:
 */
public interface HandlerService {
    public void execute(JsonObject gson);
}