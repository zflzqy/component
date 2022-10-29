package cn.zflzqy.binlog.parameter;

import com.alibaba.fastjson2.JSONArray;
import org.apache.flink.api.java.utils.ParameterTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zfl
 * @Date: 2022-07-24-9:26
 * @Description: 参数解析
 */
public abstract class AbstractParameterParseHandler {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractParameterParseHandler.class);
    /** 下个处理者*/
    private AbstractParameterParseHandler next;
    /**
     * 处理请求的方法
     * @param parameterTool 命令行参数信息
     * @return JSONArray 配置json数组信息
     */
    public abstract JSONArray handleRequest(ParameterTool parameterTool);

    public AbstractParameterParseHandler getNext() {
        return next;
    }

    public void setNext(AbstractParameterParseHandler next) {
        this.next = next;
    }
}
