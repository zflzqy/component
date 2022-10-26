package cn.zflzqy.readmysqlbinlog.parameter;

import com.alibaba.fastjson2.JSONArray;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:26
 * @Description: 参数解析
 */
public abstract class ParameterParseHandler {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ParameterParseHandler.class);
    // 下个处理者
    private ParameterParseHandler next;
    //处理请求的方法
    public abstract JSONArray handleRequest(ParameterTool parameterTool);

    public ParameterParseHandler getNext() {
        return next;
    }

    public void setNext(ParameterParseHandler next) {
        this.next = next;
    }
}
