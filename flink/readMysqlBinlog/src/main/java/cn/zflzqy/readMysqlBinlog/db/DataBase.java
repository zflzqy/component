package cn.zflzqy.readMysqlBinlog.db;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: zfl
 * @Date: 2022-07-23-17:12
 * @Description:
 */
@Data
public class DataBase implements Serializable {
    // ip
    private String ip;
    // 端口
    private Integer port;
    // 用户名
    private String username;
    // 密码
    private String password;
    // 数据库名称
    private String databaseName;
}
