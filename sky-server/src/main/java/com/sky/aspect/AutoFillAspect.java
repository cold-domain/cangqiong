package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，用于实现公共字段的自动填充功能
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 定义切入点，拦截所有使用@AutoFill注解的方法
     */
    //此处是使用了execution表达式，对mapper包下的所有类的所有方法进行拦截
    // 但是实际不需要全部拦截，只需要拦截插入和更新方法
    /*@Pointcut("execution(* com.sky.mapper.*.*(..))")
    public void autoFillPointCut(){}*/


    @Pointcut("execution(* com.sky.mapper.*.*(..))" +
            "&& @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}


    /**
     * 前置通知，在目标方法执行前调用，在通知中填写好公共字段
     */
    @Before("autoFillPointCut()")
    public void AutoFill(JoinPoint joinPoint){         //连接点
        log.info("开始进行公共字段的自动填充...");

        //获取执行方法的操作类型
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();  //获取方法签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);  //获取方法上的注解对象
        OperationType operationType = autoFill.value(); //获取操作类型

        //获取方法的参数类型--实体对象
        Object[] args = joinPoint.getArgs();        //此处要先约定mapper方法的第一个参数为实体对象
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据当前的操作类型，为对应的指定字段通过反射进行赋值
        if(operationType == OperationType.INSERT){
            //4个公共字段赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射为对象属性赋值
                setCreateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if(operationType == OperationType.UPDATE){
            //2个公共字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射为属性赋值
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }
}
