#帮助文档
readMysqlBinlog 模块
注意：
1.配置mapping.json文件，
mysql实列A同步数据到mysql实例B(该操作未解决实列同步的问题，会有bug，建议不要使用)
参照如下设置

```
[
  {
    "type": "mysql-binlog",
    "ip":"127.0.0.1",
    "port":3306,
    "dataBaseName":"myapp",
    "username": "root",
    "password": "123456",
    "tableName":".*",
    "tableMappings": [
      {
        "type": "mysql",
        "ip":"127.0.0.1",
        "port":3307,
        "username": "root",
        "password": "123456"
      }
    ]
  },
    {
    "type": "mysql-binlog",
    "ip":"127.0.0.1",
    "port":3306,
    "dataBaseName":"myapp1",
    "username": "root",
    "password": "123456",
    "tableName":".*",
    "tableMappings": [
      {
        "type": "mysql",
        "ip":"127.0.0.1",
        "port":3307,
        "username": "root",
        "password": "123456"
      }
    ]
  }
]
```

2、同步库A到库B(保证A,B库结构一致)
includeDDL:是否包含ddl语句执行，默认false，请注意ddl语句使用的时候，保证2个表的结构一致性
includeSchema：是否包含结构，用于解决部分日期查询和插入问题
```
[
  {
    "type": "mysql-binlog",
    "ip":"127.0.0.1",
    "port":3306,
    "dataBaseName":"myapp",
    "username": "root",
    "password": "123456",
    "tableName":".*",   
    "includeDDL": true,
    "includeSchema": true,
    "tableMappings": [
      {
        "type": "mysql",
        "ip":"127.0.0.1",
        "port":3306,
        "username": "root",
        "password": "123456",
        "dataBaseName":"myapp1"
      }
    ]
  }
]
```

3、同步实列A中的A1库的A2表到实列B中的B2库的B2表

```
columnMappings[
    {
        "type": "mysql-binlog",
        "ip": "127.0.0.1",
        "port": 3306,
        "dataBaseName": "myapp",
        "username": "root",
        "password": "123456",
        "tableName": "resource",
        "tableMappings": [
            {
                "type": "mysql",
                "ip": "127.0.0.1",
                "port": 3306,
                "username": "root",
                "password": "123456",
                "dataBaseName": "test1",
                "tables": [
                    {
                        "table": "resource",
                        "tableId": "id",
                        "columnMappings": {
                            "id": "id",
                            "create_time": "create_time",
                            "file_id": "file_id",
                            "name": "name",
                            "type": "type",
                            "version": "version"
                        }
                    }
                ]
            },
            {
                "type": "mysql",
                "ip": "127.0.0.1",
                "port": 3306,
                "username": "root",
                "password": "123456",
                "dataBaseName": "test2",
                "tables": [
                    {
                        "table": "resource",
                        "tableId": "id",
                        "columnMappings": {
                            "id": "id",
                            "create_time": "create_time",
                            "file_id": "file_id",
                            "name": "name",
                            "type": "type",
                            "version": "version"
                        }
                    }
                ]
            }
        ]
    }
]
```

其中columnMappings为字段映射，可根据自己的需求映射到不同的表
