package cn.zflzqy.readMysqlBinlog.parameter;

import cn.zflzqy.readMysqlBinlog.parameter.impl.JsonFileParseHandler;
import cn.zflzqy.readMysqlBinlog.parameter.impl.PropertitesFileParseHandler;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.utils.ParameterTool;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:25
 * @Description:
 */
public class ParameterFactory {
    private ParameterTool parameterTool;
    // 参数解析处理者
    private ParameterParseHandler parameterParseHandler;

    public ParameterFactory(String[] args) {
        // json文件处理
        JsonFileParseHandler jsonFileParseHandler = new JsonFileParseHandler();
        // 配置文件处理
        PropertitesFileParseHandler propertitesFileParseHandler = new PropertitesFileParseHandler();
        jsonFileParseHandler.setNext(propertitesFileParseHandler);
        parameterTool =  ParameterTool.fromArgs(args);
        // 复制第一个处理者
    }
    public JSONObject getResult(){
        return parameterParseHandler.handleRequest(parameterTool);
    }
}
