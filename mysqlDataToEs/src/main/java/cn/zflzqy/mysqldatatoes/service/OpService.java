package cn.zflzqy.mysqldatatoes.service;

import cn.zflzqy.mysqldatatoes.enums.OpEnum;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zfl
 * @Date: 2022-07-26-19:39
 * @Description:
 */
public interface OpService {

    public static final Logger LOGGER = LoggerFactory.getLogger(OpEnum.class);


    /**
     * 获取执行sql
     * @param data:数据
     * @return
     */
    String doOp(Gson data);
}
