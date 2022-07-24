/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.zflzqy.readMysqlBinlog;

import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.parameter.ParameterFactory;
import cn.zflzqy.readMysqlBinlog.sink.JdbcTemplateSink;
import com.alibaba.fastjson2.JSONObject;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.flink.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>
 * 从外部配置读取文件信息 同时可以提供多种方式读取外部配置信息，比如http,数据库，文件，以策略模式搞，同时可以考虑不同模式的合并
 * 一个binglo表日志，写入到一个或多个库或表中，支持kafka和mysqlbinlog日志两种方式，以工厂模式构建
 * 增加字段映射示例
 */

public class StreamingJob {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingJob.class);
	public static void main(String[] args) throws Exception {
		// 解析配置文件
		ParameterFactory parameterFactory = new ParameterFactory(args);
		// 获取解析数据
		JSONObject result = parameterFactory.getResult();
		// 订阅binglog/kafka,构建连接池，处理数据
		MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
				.hostname("192.168.50.102")
				.port(3306)
				.databaseList("myapp") // set captured database
				.tableList("myapp.resource") // set captured table
				.username("root")
				.password("123456")
				.deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
				.build();

		// 启动监听库：库名：配置信息作为key

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// enable checkpoint
		env.enableCheckpointing(3000);
		// 构建数据库连接信息
		DataBase myAppDataBase = new DataBase();
		myAppDataBase.setIp("192.168.50.102");
		myAppDataBase.setPort(3306);
		myAppDataBase.setUsername("root");
		myAppDataBase.setPassword("123456");
		myAppDataBase.setDatabaseName("myapp");

		env.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
			.setParallelism(1)
			.flatMap(new RichFlatMapFunction<String, Object>() {
					@Override
					public void flatMap(String s, Collector<Object> collector) throws Exception {
						// 将得到数据，反射调用方法，传入jdbTemplate和数据源，可以同时提供事务级处理
						LOGGER.info("得到的数据：{}",s);
						collector.collect(s);
					}
				})
			.addSink(new JdbcTemplateSink<>(myAppDataBase));

		env.execute("Print MySQL Snapshot + Binlog");
	}
}
