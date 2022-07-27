package cn.zflzqy.readMysqlBinlog.sink.service;

import cn.zflzqy.readMysqlBinlog.sink.enums.OpEnum;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: zfl
 * @Date: 2022-07-26-19:39
 * @Description:
 */
public interface OpService {

    public static final Logger LOGGER = LoggerFactory.getLogger(OpEnum.class);
    List<Tuple2<String,List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping);
}
