package cn.zflzqy.binlog.sink.mq;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * @Author: zfl
 * @Date: 2022-07-24-11:53
 * @Description:
 */
public class KafkaSink<IN> extends RichSinkFunction<IN> {
    @Override
    public void invoke(IN value, Context context) throws Exception {
        super.invoke(value, context);
    }
}
