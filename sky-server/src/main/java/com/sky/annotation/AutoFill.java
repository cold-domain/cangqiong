package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识需要自动填充的方法
 */
@Target(ElementType.METHOD)     // 指定注解在方法上使用
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //指定数据库操作的类型
    OperationType value();
}
