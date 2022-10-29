package cn.zflzqy.binlog.source.enums;


/**
 * @Author: zfl
 * @Date: 2022-07-26-19:36
 * @Description:
 */
public enum DataTypeEnum {
    /** kafka数据源*/
    kafka("kafka"),
    /** mysqlbinlog数据源*/
    mysqlBingLog("mysql-binlog");

    public String enumField;

    DataTypeEnum(String enumField) {
        this.enumField = enumField;
    }
}
