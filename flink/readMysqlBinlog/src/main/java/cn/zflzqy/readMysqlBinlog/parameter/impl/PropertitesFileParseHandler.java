package cn.zflzqy.readMysqlBinlog.parameter.impl;

import cn.zflzqy.readMysqlBinlog.parameter.ParameterParseHandler;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.utils.ParameterTool;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:27
 * @Description:命令行解析
 */
public class PropertitesFileParseHandler extends ParameterParseHandler {
    @Override
    public JSONArray handleRequest(ParameterTool parameterTool)  {
        String configPath = parameterTool.get("configPath");
        if (null!=configPath&&FileUtils.fileExists(configPath)){
            // 解析参数信息
            ParameterTool propertiesFile = null;
            try {
                propertiesFile = ParameterTool.fromPropertiesFile(configPath);
                // 需要将propertites转换成可识别的json结构 todo 暂未实现
                return new JSONArray(propertiesFile.toMap());
            } catch (IOException e) {
                LOGGER.warn("解析配置文件异常：",e);
            }
        }
        // 提交给下个处理者
        return this.getNext().handleRequest(parameterTool);
    }
}
