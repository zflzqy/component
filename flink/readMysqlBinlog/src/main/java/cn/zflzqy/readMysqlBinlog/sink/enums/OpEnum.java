package cn.zflzqy.readMysqlBinlog.sink.enums;

import cn.zflzqy.readMysqlBinlog.sink.service.OpService;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.*;

/**
 * @Author: zfl
 * @Date: 2022-07-26-19:36
 * @Description:
 */
public enum OpEnum implements OpService {
    // 读取
    r {
        @Override
        public List<Tuple2<String, List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
            return c.doOp(data,idField,tableName,tableMapping);
        }
    },
    // 创建
    c {
        @Override
        public List<Tuple2<String, List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
            List<Tuple2<String, List<Object>>> sqls = new ArrayList<>();
            // 构建查询语句
            sqls.add(this.getQuery(data, idField, tableName, tableMapping));
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 构建插入语句
            StringBuilder insertSql = new StringBuilder("insert into `" + tableName + "` (");
            Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
            JSONObject newData = data.getJSONObject("after");
            while (iterator.hasNext()) {
                Map.Entry<String, Object> mapping = iterator.next();
                insertSql.append("`").append(mapping.getValue()).append("`,");
                args.add(newData.get(mapping.getKey()));
            }
            insertSql.deleteCharAt(insertSql.length() - 1);
            insertSql.append(") values(");
            // 加入参数
            for (int i=0;i<tableMapping.size();i++){
                insertSql.append("?").append(",");
            }
            insertSql.deleteCharAt(insertSql.length() - 1);
            insertSql.append(")");
            LOGGER.info("插入语句：{},参数：{}", insertSql.toString(),JSONObject.toJSONString(args));
            Tuple2<String, List<Object>> insert = new Tuple2<>(insertSql.toString(), args);
            sqls.add(insert);
            return sqls;
        }
    },
    // 更新
    u {
        @Override
        public List<Tuple2<String, List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
            List<Tuple2<String, List<Object>>> sqls = new ArrayList<>();
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 更新语句
            StringBuilder updateSql = new StringBuilder();
            updateSql.append("UPDATE `" + tableName + "` SET ");

            Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
            JSONObject newData = data.getJSONObject("after");
            while (iterator.hasNext()) {
                Map.Entry<String, Object> mapping = iterator.next();
                if (!StringUtils.equals((CharSequence) mapping.getValue(), idField)) {
                    updateSql.append("`").append(mapping.getValue()).append("` = ?,");
                    args.add(newData.get(mapping.getKey()));
                }
            }
            updateSql.deleteCharAt(updateSql.length() - 1);

            // 构建where条件
            updateSql.append(" WHERE `" + idField + "` = ?");
            iterator = tableMapping.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if (StringUtils.equals((CharSequence) entry.getValue(), idField)) {
                    args.add(newData.getString(entry.getKey()));
                }
            }
            LOGGER.info("更新语句：{},参数：{}", updateSql.toString(),JSONObject.toJSONString(args));

            Tuple2<String, List<Object>> update = new Tuple2<>(updateSql.toString(), args);
            sqls.add(update);
            return sqls;
        }
    },
    // 删除
    d {
        @Override
        public List<Tuple2<String, List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
            List<Tuple2<String, List<Object>>> sqls = new ArrayList<>();
            List<Object> args = new ArrayList<>();

            JSONObject oldData = data.getJSONObject("before");
            Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if (StringUtils.equals((CharSequence) entry.getValue(), idField)) {
                    args.add(oldData.getString(entry.getKey()));
                }
            }
            Tuple2<String, List<Object>> rs = new Tuple2<>("delete from  `" + tableName + "`  where `" + idField + "` = ?", args);
            sqls.add(rs);
            LOGGER.info("删除语句：{},参数：{}", rs.f0.toString(),JSONObject.toJSONString(args));
            return sqls;
        }
    };

    public Tuple2<String, List<Object>> getQuery(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
        JSONObject newData = data.getJSONObject("after");
        List<Object> args = new ArrayList<>();
        Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            if (StringUtils.equals((CharSequence) entry.getValue(), idField)) {
                args.add(newData.getString(entry.getKey()));
            }
        }
        Tuple2<String, List<Object>> rs = new Tuple2<>("select * from `" + tableName + "`  where `" + idField + "` = ?", args);
        LOGGER.info("查询语句：{},参数:{}", rs.f0.toString(),JSONObject.toJSONString(args));
        return rs;
    }

}
