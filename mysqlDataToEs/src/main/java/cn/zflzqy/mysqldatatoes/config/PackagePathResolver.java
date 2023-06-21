package cn.zflzqy.mysqldatatoes.config;

import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
        SpringApplication app = event.getSpringApplication();
        Class<?> mainApplicationClass = app.getMainApplicationClass();
        if (mainApplicationClass != null) {
            String mainPackagePath = mainApplicationClass.getPackage().getName();
            mainClassPackagePath = mainPackagePath;
            log.debug("The package path of the main class is: {}",mainPackagePath);
        }
    }
}
