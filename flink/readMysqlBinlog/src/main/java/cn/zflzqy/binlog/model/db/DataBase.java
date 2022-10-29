package cn.zflzqy.binlog.model.db;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * @Author: zfl
 * @Date: 2022-07-23-17:12
 * @Description:
 */
@Data
public class DataBase implements Serializable {
    /** ip*/
    private String ip;
    /** 端口 */
    private Integer port;
    /** 用户名*/
    private String username;
    /** 密码*/
    private String password;
    /** 数据库名称*/
    private String databaseName;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataBase dataBase = (DataBase) o;
        return Objects.equals(ip, dataBase.ip) && Objects.equals(port, dataBase.port) && Objects.equals(username, dataBase.username) && Objects.equals(password, dataBase.password) && Objects.equals(databaseName, dataBase.databaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, username, password, databaseName);
    }
}
