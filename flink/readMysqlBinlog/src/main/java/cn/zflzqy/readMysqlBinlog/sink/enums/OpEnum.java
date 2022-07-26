package cn.zflzqy.readMysqlBinlog.sink.enums;

import cn.zflzqy.readMysqlBinlog.sink.service.OpService;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zfl
 * @Date: 2022-07-26-19:36
 * @Description:
 */
public enum OpEnum  implements OpService {
    // 创建
    c{
        @Override
        public List<Tuple2<String,List<Object>>> doOp(JSONObject data, String idField, String tableName, JSONObject tableMapping) {
            List<Tuple2<String,List<Object>>> sqls = new ArrayList<>();
            // 构建查询语句
            sqls.add(this.getQuery(data,idField,tableName,tableMapping));
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 构建插入语句
            StringBuilder insertSql = new StringBuilder("insert into `"+tableName+"` (");
            tableMapping.entrySet().stream().map(mapping->{
                insertSql.append("`").append(mapping.getValue()).append("`,");
                args.add(data.get(mapping.getKey()));
                return null;
            });
            insertSql.deleteCharAt(insertSql.length()-1);
            insertSql.append(") values(");
            tableMapping.entrySet().stream().map(c->{
                insertSql.append("?").append(",");
                return  null;
            });
            insertSql.deleteCharAt(insertSql.length()-1);
            insertSql.append(")");
            LOGGER.info("插入语句：{}",insertSql.toString());
            tableMapping.entrySet().forEach(entry -> {

            });
            Tuple2<String,List<Object>> insert = new Tuple2<>(insertSql.toString(),args);
            return sqls;
        }
    },
    // 更新
    u{
        @Override
        public List<Tuple2<String,List<Object>>> doOp(JSONObject data, String idField,String tableName,JSONObject tableMapping) {
            List<Tuple2<String,List<Object>>> sqls = new ArrayList<>();
            // 查询语句
            sqls.add(this.getQuery(data,idField,tableName,tableMapping));
            // 构建参数
            List<Object> args = new ArrayList<>();
            // 更新语句
            StringBuilder updateSql = new StringBuilder();
            updateSql.append("UPDATE `"+tableName+"` SET ");
            tableMapping.entrySet().stream().map(mapping->{
                if (!StringUtils.equals((CharSequence) mapping.getValue(),idField)) {
                    updateSql.append("`").append(mapping.getValue()).append("` = ?,");
                    args.add(data.get(mapping.getKey()));
                }
                return null;
            });
            // 构建where条件
            updateSql.append(" WHERE `"+idField+"` = ?");
            tableMapping.entrySet().forEach(entry -> {
                if (StringUtils.equals((CharSequence) entry.getValue(),idField)){
                    args.add(data.getString(entry.getKey()));
                }
            });
            LOGGER.info("更新语句：{}",updateSql.toString());
            return sqls;
        }
    },
    // 删除
    d{
        @Override
        public List<Tuple2<String,List<Object>>> doOp(JSONObject data, String idField,String tableName,JSONObject tableMapping) {
            List<Tuple2<String,List<Object>>> sqls = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            tableMapping.entrySet().forEach(entry -> {
                if (StringUtils.equals((CharSequence) entry.getValue(),idField)){
                    args.add(data.getString(entry.getKey()));
                }
            });
            Tuple2<String,List<Object>> rs = new Tuple2<>("delete from  `"+tableName+"`  where `"+idField+"` = ?",args);
            return sqls;
        }
    };
    public Tuple2<String,List<Object>> getQuery(JSONObject data, String idField,String tableName,JSONObject tableMapping){
        List<Object> args = new ArrayList<>();
        tableMapping.entrySet().forEach(entry -> {
            if (StringUtils.equals((CharSequence) entry.getValue(),idField)){
                args.add(data.getString(entry.getKey()));
            }
        });
        Tuple2<String,List<Object>> rs = new Tuple2<>("select * from `"+tableName+"`  where `"+idField+"` = ?",args);
        return rs;
    }

}
