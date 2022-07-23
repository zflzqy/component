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

import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="https://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class StreamingJob {

	public static void main(String[] args) throws Exception {
		MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
				.hostname("127.0.0.1")
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

		env
				.fromSource(mySqlSource, WatermarkStrategy.noWatermarks(), "MySQL Source")
				// set 4 parallel source tasks
				.setParallelism(1)
				.print().setParallelism(1); // use parallelism 1 for sink to keep message ordering

		env.execute("Print MySQL Snapshot + Binlog");
	}
}
