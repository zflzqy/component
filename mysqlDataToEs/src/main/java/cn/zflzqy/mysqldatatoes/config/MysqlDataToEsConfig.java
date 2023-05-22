package cn.zflzqy.mysqldatatoes.config;

import cn.zflzqy.mysqldatatoes.propertites.MysqlDataToEsPropertites;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MysqlDataToEsPropertites.class)
public class MysqlDataToEsConfig  {

}