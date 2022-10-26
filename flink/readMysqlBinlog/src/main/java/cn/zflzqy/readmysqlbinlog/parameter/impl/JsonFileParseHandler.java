package cn.zflzqy.readmysqlbinlog.parameter.impl;

import cn.zflzqy.readmysqlbinlog.parameter.ParameterParseHandler;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:27
 * @Description:命令行解析
 */
@Slf4j
public class JsonFileParseHandler extends ParameterParseHandler {
    @Override
    public JSONArray handleRequest(ParameterTool parameterTool) {
        String jsonPath = parameterTool.get("jsonPath");
        log.debug("准备解析地址为{}的json文件数据",jsonPath);
        // 解析参数信息
        try {
            return JSONArray.parseArray(FileUtils.fileRead(jsonPath));
        } catch (IOException e) {
            LOGGER.warn("解析json文件异常：", e);
        }
        // 提交给下个处理者
        return this.getNext().handleRequest(parameterTool);
    }
}
