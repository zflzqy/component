package cn.zflzqy.mysqldatatoes.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author zflzqy
 */

public class PackagePathResolver implements ApplicationListener<ApplicationStartingEvent> {
    private static final Logger log = LoggerFactory.getLogger(PackagePathResolver.class);

    public static String  mainClassPackagePath;

    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        // 添加映射属性 todo，在处理器中检查是否存在该字段，检查字段的属性
        SpringApplication app = event.getSpringApplication();
        Class<?> mainApplicationClass = app.getMainApplicationClass();
        if (mainApplicationClass != null) {
            String mainPackagePath = mainApplicationClass.getPackage().getName();
            mainClassPackagePath = mainPackagePath;
            log.debug("The package path of the main class is: {}",mainPackagePath);
        }
    }
}
