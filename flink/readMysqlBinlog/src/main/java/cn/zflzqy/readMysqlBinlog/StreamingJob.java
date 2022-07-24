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
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.DataStreamUtils;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.FileUtils;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.File;
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
		// 构建流与映射
		List<Tuple2<DataStreamSource<String>, JSONArray>> dataStreamSource = DataStreamStrategy.getDataStreamSource(config, env);
		// 构建数据库信息对象
		dataStreamSource.forEach(dataStreamSourceJSONArrayTuple2 -> {
			DataStreamSource<String> dataStreamSource1 = dataStreamSourceJSONArrayTuple2._1;
			for (int i =0;i<dataStreamSourceJSONArrayTuple2._2.size();i++){
				OutputTag<String> outputTag = new OutputTag<String>("stream"+i, Types.STRING) {};
				JSONObject jsonObject = dataStreamSourceJSONArrayTuple2._2.getJSONObject(i);
				// 构建连接池
				DataBase dataBase =new DataBase();
				dataBase.setIp(jsonObject.getString("ip"));
				dataBase.setPort(jsonObject.getInteger("port"));
				dataBase.setDatabaseName(jsonObject.getString("dataBaseName"));
				dataBase.setUsername(jsonObject.getString("username"));
				dataBase.setPassword(jsonObject.getString("password"));
				// todo 修改流复制为多表，可以通过配置的字段映射先构建数据库环境，同时启用事务
				// 流复制
				SingleOutputStreamOperator streamOperator = dataStreamSource1.process(new ProcessFunction<String, Object>() {
					@Override
					public void processElement(String s, ProcessFunction<String, Object>.Context context, Collector<Object> collector) throws Exception {
						collector.collect(s);
						context.output(outputTag,s);
					}
				});
				DataStream<String> sideOutput = streamOperator.getSideOutput(outputTag);
				sideOutput.addSink(new JdbcTemplateSink(dataBase));
			}

		});
		// 对映射中的数据进行初始化数据库池 todo
		// enable checkpoint
		env.enableCheckpointing(3000);
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
