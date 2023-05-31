package cn.zflzqy.mysqldatatoes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

/**
 * @Author: zfl
 * @Date: 2023-05-10-16:29
 * @Description:
 */
public class PackageScan {
    private static final Logger log = LoggerFactory.getLogger(PackageScan.class);
    private static Map<String,Class> indexs = new HashMap();

    public static Map<String, Class> getIndexs() {
        return indexs;
    }

    /**
     * 扫描实体包并检查是否存在租户id
     * @param packageName:包名
     */
    public static void scanEntities(String packageName) {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace('.', '/');

        try {
            // 获取包路径
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                    scanEntitiesInDirectory(packageName, directory);
                }
            }
        } catch (IOException e) {
            log.error("io异常", e);
        }
    }


    private static void scanEntitiesInDirectory(String packageName, File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String subPackageName = packageName + "." + file.getName();
                    scanEntitiesInDirectory(subPackageName, new File(file.getPath()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (isEntityClass(clazz)) {
                            indexs.put(getIndexName(clazz),clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error("类未找到异常", e);
                    }
                }
            }
        }
    }

    /**
     * 检查是否为符合条件的实体类
     * @param clazz
     * @return
     */
    private static boolean isEntityClass(Class<?> clazz) {
        // 判断是否带有@Document注解
        return clazz.isAnnotationPresent(Document.class);
    }

    /**
     * 获取索引名
     * @param clazz
     * @return
     */
    private static String getIndexName(Class<?> clazz) {
        Annotation annotation = clazz.getAnnotation(Document.class);
        if (annotation instanceof Document) {
            return ((Document) annotation).indexName();
        }
        return null;
    }
}


