package cn.zflzqy.binlog.parameter;

import cn.zflzqy.binlog.parameter.impl.JsonFileParseHandler;
import cn.zflzqy.binlog.parameter.impl.PropertiesFileParseHandler;
import com.alibaba.fastjson2.JSONArray;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:25
 * @Description:
 */
public class ParameterFactory {
    private ParameterTool parameterTool;
    /** 参数解析处理者 */
    private AbstractParameterParseHandler parameterParseHandler;

    public ParameterFactory(String[] args) {
        // json文件处理
        JsonFileParseHandler jsonFileParseHandler = new JsonFileParseHandler();
        // 配置文件处理
        PropertiesFileParseHandler propertiesFileParseHandler = new PropertiesFileParseHandler();
        jsonFileParseHandler.setNext(propertiesFileParseHandler);
        parameterTool =  ParameterTool.fromArgs(args);
        // 定义第一个处理者
        parameterParseHandler = jsonFileParseHandler;
    }
    public JSONArray getResult(){
        return parameterParseHandler.handleRequest(parameterTool);
    }
}
