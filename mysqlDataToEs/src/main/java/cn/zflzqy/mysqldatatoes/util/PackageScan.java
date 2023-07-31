package cn.zflzqy.mysqldatatoes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @Author: zfl
 * @Date: 2023-05-10-16:29
 * @Description: 包扫描器
 */
public class PackageScan {
    private static final Logger log = LoggerFactory.getLogger(PackageScan.class);
    // 索引数据
    private static final Map<String,Class> INDEXES = new HashMap();

    public static Map<String, Class> getIndexes() {
        return INDEXES;
    }

    /**
     * 扫描实体包并检查是否存在租户id
     * @param packageName:包名
     */
    public static void scanEntities(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace('.', '/');
        String packageResource = packagePath + "/";
        // 获取包路径
        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(packageResource);
        } catch (IOException e) {
            log.error("IO 异常：", e);
        }
        if (resources == null) {
            return;
        }
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            scanEntitiesInDirectory(packageName, resource);
        }

        log.info("扫描的索引信息：{}", INDEXES.toString());
    }


    /**
     * 扫描包下的实体
     * @param packageName：包名
     * @param directoryURL：文件路径
     */
    private static void scanEntitiesInDirectory(String packageName, URL  directoryURL) {
        try {
            URI uri = directoryURL.toURI();
            if (uri.getScheme().equals("file")) {
                File directory = new File(uri);
                scanEntitiesInDirectory(packageName, directory);
            } else if (uri.getScheme().equals("jar")) {
                JarURLConnection jarURLConnection = (JarURLConnection) directoryURL.openConnection();
                try (JarFile jarFile = jarURLConnection.getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (entryName.startsWith(packageName.replace('.', '/')) && entryName.endsWith(".class")) {
                            String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                            try {
                                Class<?> clazz = Class.forName(className);
                                if (isEntityClass(clazz)) {
                                    INDEXES.put(getIndexName(clazz),clazz);
                                }
                            } catch (ClassNotFoundException e) {
                                log.error("类未找到异常", e);
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("IO 异常", e);
        }
    }

    /**
     * 扫描包下的实体
     * @param packageName：包名
     * @param directory：文件路径
     */
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
                            INDEXES.put(getIndexName(clazz),clazz);
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
        boolean annotationPresent = clazz.isAnnotationPresent(Document.class);

        if (annotationPresent){
            Class aClass = INDEXES.get(getIndexName(clazz));
            if (aClass == null){
                return  true;
            }
            // 如果已有的数据是当前类的父类，则返回false
            if (aClass.isAssignableFrom(clazz)){
                return  false;
            };
            return true;
        }
        return false;
    }

    /**
     * 获取索引名
     * @param clazz
     * @return
     */
    private static String getIndexName(Class<?> clazz) {
        Annotation annotation = clazz.getAnnotation(Document.class);
        if (annotation != null) {
            return ((Document) annotation).indexName();
        }
        return null;
    }
}



