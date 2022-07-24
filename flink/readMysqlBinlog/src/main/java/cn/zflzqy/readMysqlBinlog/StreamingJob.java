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

import cn.zflzqy.readMysqlBinlog.dataStreamSource.DataStreamStrategy;
import cn.zflzqy.readMysqlBinlog.db.DataBase;
import cn.zflzqy.readMysqlBinlog.parameter.ParameterFactory;
import cn.zflzqy.readMysqlBinlog.sink.JdbcTemplateSink;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.List;

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
		JSONArray config = parameterFactory.getResult();
		// 订阅binglog/kafka,构建连接池，处理数据
		// 启动监听库：库名：配置信息作为key
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setParallelism(1);
		env.enableCheckpointing(3000);
		// 构建流与映射
		List<Tuple2<DataStreamSource<String>, JSONArray>> dataStreamSource = DataStreamStrategy.getDataStreamSource(config, env);
		// 构建数据库信息对象
		dataStreamSource.forEach(dataStreamSourceJSONArrayTuple2 -> {
			DataStreamSource<String> dataStreamSource1 = dataStreamSourceJSONArrayTuple2._1;
			for (int i =0;i<dataStreamSourceJSONArrayTuple2._2.size();i++){
				OutputTag<String> outputTag = new OutputTag<String>("stream"+i, Types.STRING) {};
				// todo 根据类型策略化输出模式
				JSONObject jsonObject = dataStreamSourceJSONArrayTuple2._2.getJSONObject(i);
				// 构建连接池
				DataBase dataBase =new DataBase();
				String ip = jsonObject.getString("ip");
				int port = jsonObject.getInteger("port");
				String dataBaseName = jsonObject.getString("dataBaseName");

				dataBase.setIp(ip);
				dataBase.setPort(port);
				dataBase.setDatabaseName(dataBaseName);
				dataBase.setUsername(jsonObject.getString("username"));
				dataBase.setPassword(jsonObject.getString("password"));
				// 流复制 todo 多表
				JSONArray tables = jsonObject.getJSONArray("tables");
				for (int j=0;j<tables.size();j++) {
					// 获取表配置
					JSONObject tablesJSONObject = tables.getJSONObject(j);
					SingleOutputStreamOperator<String> streamOperator = dataStreamSource1.process(new ProcessFunction<String, String>() {
						@Override
						public void processElement(String s, ProcessFunction<String, String>.Context context, Collector<String> collector) throws Exception {
							collector.collect(s);
							context.output(outputTag, s);
						}
					});
					streamOperator.flatMap(new FlatMapFunction<String, Object>() {
						@Override
						public void flatMap(String s, Collector<Object> collector) throws Exception {
							// 此次将类型处理为
							LOGGER.info("将{}处理到{}.{},映射关系：{}",s,dataBaseName,tablesJSONObject.getString("table"),tablesJSONObject.getString("columnMappings"));
							// 最终形成sql语句
						}
					});
					DataStream<String> sideOutput = streamOperator.getSideOutput(outputTag);
					sideOutput.addSink(new JdbcTemplateSink(dataBase));
				}
			}

		});
		// 对映射中的数据进行初始化数据库池 todo
		// enable checkpoint
//			.setParallelism(1)
//			.flatMap(new RichFlatMapFunction<String, Object>() {
//					@Override
//					public void flatMap(String s, Collector<Object> collector) throws Exception {
//						// 将得到数据，反射调用方法，传入jdbTemplate和数据源，可以同时提供事务级处理
//						LOGGER.info("得到的数据：{}",s);
//						collector.collect(s);
//					}
//				})
//			.addSink(new JdbcTemplateSink<>(myAppDataBase));

		env.execute("Print MySQL Snapshot + Binlog");
	}
}
