package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.NavigableSet;


@Aspect
@Component
public class AutoFillAspect {
    private final Logger log = LoggerFactory.getLogger(AutoFillAspect.class);

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    void pt() {}

    @Before("pt()")
    // TODO 补完反射知识后完成
    public void autoFill(JoinPoint joinPoint) {
        // 获得注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        AutoFill annotation = method.getAnnotation(AutoFill.class);

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length == 0) {
            return;
        }
        assert args != null;
        Object entity = args[0];
        Class<?> aClass = entity.getClass();

        if (annotation.value() == OperationType.INSERT) {
            try {
                Method setCreateTime = aClass.getMethod("setCreateTime", LocalDateTime.class);
                Method setUpdateTime = aClass.getMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = aClass.getMethod("setUpdateUser", Long.class);
                Method setCreateUser = aClass.getMethod("setCreateUser", Long.class);

                // 反射赋值
                setCreateTime.invoke(entity, LocalDateTime.now());
                setUpdateTime.invoke(entity, LocalDateTime.now());
                setCreateUser.invoke(entity, BaseContext.getCurrentId());
                setUpdateUser.invoke(entity, BaseContext.getCurrentId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (annotation.value() == OperationType.UPDATE) {
            try {
                Method setUpdateTime = aClass.getMethod("setUpdateTime", LocalDateTime.class);
                Method setUpdateUser = aClass.getMethod("setUpdateUser", Long.class);

                // 反射赋值
                setUpdateTime.invoke(entity, LocalDateTime.now());
                setUpdateUser.invoke(entity, BaseContext.getCurrentId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        log.info("反射：" + entity);
    }
}
