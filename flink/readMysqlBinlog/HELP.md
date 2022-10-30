#帮助文档

注意：

1、数据库中的日期字段暂时由于底层的问题存在转换异常，建议使用varchar作为日期

2、未开发ddl语句执行，请自行保证库结构一致

1.配置mapping.json文件，
mysql实列A同步数据到mysql实例B

```
[
  {
    "type": "mysql-binlog",
    "ip":"127.0.0.1",
    "port":3306,
    "dataBaseName":".*",
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
