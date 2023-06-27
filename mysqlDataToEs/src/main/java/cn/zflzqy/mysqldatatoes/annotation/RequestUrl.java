package cn.zflzqy.mysqldatatoes.annotation;

import java.lang.annotation.*;

/**
 * 注解在类上用于添加前端请求信息的，支持当前实体中的参数注入
 * @author zflzqy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface RequestUrl {
    // 请求地址
    String requestUrl() default "";

}
