package cn.zflzqy.binlog.transform.enums;

import cn.zflzqy.binlog.transform.service.OpService;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.springframework.util.CollectionUtils;

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
            // 如果没用配置表，就从数据种获取
            if (StringUtils.isBlank(tableName)) {
                tableName = getTableName(data);
            }
            List<Tuple2<String, List<Object>>> sqls = new ArrayList<>();
            // 构建查询语句
            sqls.add(this.getQuery(data, idField, tableName, tableMapping));
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 构建插入语句
            StringBuilder insertSql = new StringBuilder("insert into `" + tableName + "` (");
            JSONObject newData = data.getJSONObject("after");
            // 参数个数
            int argsCount = 0;
            if (CollectionUtils.isEmpty(tableMapping)){
                argsCount = newData.size();
                Iterator<Map.Entry<String, Object>> iterator = newData.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    insertSql.append("`").append(entry.getKey()).append("`,");
                    args.add(entry.getValue());
                }
            }else {
                argsCount = tableMapping.size();
                Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> mapping = iterator.next();
                    insertSql.append("`").append(mapping.getValue()).append("`,");
                    args.add(newData.get(mapping.getKey()));
                }

            }
            insertSql.deleteCharAt(insertSql.length() - 1);
            insertSql.append(") values(");
            // 加入参数
            for (int i=0;i<argsCount;i++){
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
            // 如果没用配置表，就从数据种获取
            if (StringUtils.isBlank(tableName)) {
                tableName = getTableName(data);
            }
            List<Tuple2<String, List<Object>>> sqls = new ArrayList<>();
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 更新语句
            StringBuilder updateSql = new StringBuilder();
            updateSql.append("UPDATE `" + tableName + "` SET ");
            JSONObject newData = data.getJSONObject("after");
            // 以前的数据
            JSONObject oldData = data.getJSONObject("before");

            // 条件语句
            StringBuilder whereSql = new StringBuilder(" WHERE ");
            if (CollectionUtils.isEmpty(tableMapping)||StringUtils.isBlank(idField)){
                Iterator<Map.Entry<String, Object>> iterator = newData.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    updateSql.append("`").append(entry.getKey()).append("` =?,");
                    args.add(entry.getValue());
                }
                Iterator<Map.Entry<String, Object>> oldIterator = oldData.entrySet().iterator();
                while (oldIterator.hasNext()) {
                    Map.Entry<String, Object> entry = oldIterator.next();
                    whereSql.append("`").append(entry.getKey()).append("` = ? and");
                    args.add(oldData.get(entry.getKey()));
                }

            }else {
                Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> mapping = iterator.next();
                    if (!StringUtils.equals((CharSequence) mapping.getValue(), idField)) {
                        updateSql.append("`").append(mapping.getValue()).append("` = ?,");
                        args.add(newData.get(mapping.getKey()));
                        whereSql.append(" `").append(idField).append("` = ?");
                    }
                }
            }

            // 删除最后一个逗号
            updateSql.deleteCharAt(updateSql.length() - 1);
            // 构建sql语句
            updateSql.append(StringUtils.removeEnd(whereSql.toString(), "and"));
            LOGGER.info("更新语句：{},参数：{}", updateSql,JSONObject.toJSONString(args));

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

            StringBuilder whereSql = new StringBuilder(" where");
            JSONObject oldData = data.getJSONObject("before");
            if (CollectionUtils.isEmpty(tableMapping)||StringUtils.isBlank(idField)) {
                Iterator<Map.Entry<String, Object>> iterator = oldData.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, Object> next = iterator.next();
                    args.add(next.getValue());
                    whereSql.append(" `").append(next.getKey()).append("` =? and");
                }
                // 如果没用配置表，就从数据种获取
                if (StringUtils.isBlank(tableName)) {
                    tableName = getTableName(data);
                }
            }else {
                Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> entry = iterator.next();
                    if (StringUtils.equals((CharSequence) entry.getValue(), idField)) {
                        args.add(oldData.getString(entry.getKey()));
                        whereSql.append(" `").append(idField).append("` = ?");
                    }
                }
            }
            Tuple2<String, List<Object>> rs = new Tuple2<>("delete from  `" + tableName+"`"+ StringUtils.removeEnd(whereSql.toString(), "and"), args);
            sqls.add(rs);
            LOGGER.info("删除语句：{},参数：{}", rs.f0.toString(),JSONObject.toJSONString(args));
            return sqls;
        }
    };

    public Tuple2<String, List<Object>> getQuery(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
        JSONObject newData = data.getJSONObject("after");
        List<Object> args = new ArrayList<>();
        // 查询条件
        StringBuilder whereSql = new StringBuilder(" where ");
        if (StringUtils.isNotBlank(idField)&& !CollectionUtils.isEmpty(tableMapping)) {
            Iterator<Map.Entry<String, Object>> iterator = tableMapping.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                if (StringUtils.equals((CharSequence) entry.getValue(), idField)) {
                    args.add(newData.getString(entry.getKey()));
                    whereSql.append("`").append(idField).append("` = ?");
                }
            }
        }else {
            // 当为配置映射信息的时候，执行全表插入
            Iterator<Map.Entry<String, Object>> iterator = newData.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, Object> entry = iterator.next();
                args.add(entry.getValue());
                whereSql.append(" `").append(entry.getKey()).append("` = ? and");
            }
            // 如果没用配置表，就从数据种获取
            if (StringUtils.isBlank(tableName)) {
                tableName = getTableName(data);
            }
        }
        // 最终的条件sql
        String conditionSql = StringUtils.removeEnd(whereSql.toString(), "and");
        Tuple2<String, List<Object>> rs = new Tuple2<>("select * from `" + tableName+"` " +conditionSql, args);
        LOGGER.info("查询语句：{},参数:{}", rs.f0.toString(),JSONObject.toJSONString(args));
        return rs;
    }

    public String getTableName(JSONObject data){
        return  data.getJSONObject("source").getString("table");
    }

}
