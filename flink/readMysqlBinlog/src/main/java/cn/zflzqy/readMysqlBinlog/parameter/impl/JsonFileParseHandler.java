package cn.zflzqy.readMysqlBinlog.parameter.impl;

import cn.zflzqy.readMysqlBinlog.parameter.ParameterParseHandler;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.utils.ParameterTool;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:27
 * @Description:命令行解析
 */
public class JsonFileParseHandler extends ParameterParseHandler {
    @Override
    public JSONObject handleRequest(ParameterTool parameterTool)  {
        String jsonPath = parameterTool.get("jsonPath");
        if (null!=jsonPath&&FileUtils.fileExists(jsonPath)){
            // 解析参数信息
            try {
                return JSONObject.parseObject(FileUtils.fileRead(jsonPath));
            } catch (IOException e) {
                LOGGER.warn("解析json文件异常：{}",e.getMessage());
            }
        }
        // 提交给下个处理者
        return this.getNext().handleRequest(parameterTool);
    }
}
