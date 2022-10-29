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

package cn.zflzqy.readmysqlbinlog;

import cn.zflzqy.readmysqlbinlog.dataStreamSource.DataStreamStrategy;
import cn.zflzqy.readmysqlbinlog.parameter.ParameterFactory;
import cn.zflzqy.readmysqlbinlog.transform.strategy.SinkContextStrategy;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>
 * 从外部配置读取文件信息 同时可以提供多种方式读取外部配置信息，比如http,数据库，文件，以策略模式搞，同时可以考虑不同模式的合并
 * 一个binglo表日志，写入到一个或多个库或表中，支持kafka和mysqlbinlog日志两种方式，以工厂模式构建
 * 入参：-jsonPath
 * 增加字段映射示例
 */

public class StreamingJob {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingJob.class);
	public static void main(String[] args) throws Exception {
		// 解析配置文件
		ParameterFactory parameterFactory = new ParameterFactory(args);
		// 获取解析数据
		JSONArray config = parameterFactory.getResult();
		// 订阅binlog/kafka,构建连接池，处理数据
		// 启动监听库：库名：配置信息作为key
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		// 获取当前cpu核心作为线程数
		env.setParallelism(Runtime.getRuntime().availableProcessors());
		env.enableCheckpointing(3000);
		// 构建流与映射
		List<Tuple2<DataStreamSource<String>, JSONArray>> dataStreamSource = DataStreamStrategy.getDataStreamSource(config, env);
		// 构建数据库信息对象
		dataStreamSource.forEach(source -> {
			DataStreamSource<String> stringDataStreamSource = source.f0;
			for (int i =0;i<source.f1.size();i++){
				// 根据类型策略化输出模式
				JSONObject jsonObject = source.f1.getJSONObject(i);
				SinkContextStrategy.execute(jsonObject,stringDataStreamSource);
			}
		});

		env.execute("read binlog to target info ");
	}
}
