package com.kline.aspect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.nosql.mongo.MongoFactory;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kline.interceptor.BodyReaderHttpServletRequestWrapper;
import com.kline.jwt.AuthStorage;
import com.kline.jwt.JwtUser;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Slf4j
@Aspect
@Component
public class RequestAspect {

    @Resource
    private MongoTemplate mongoTemplate;

    @Pointcut("execution(public * com.kline.controller.*.*(..))")
    public void log() {}

    private static final ThreadLocal<JSONObject> localLog = new ThreadLocal<JSONObject>();

    public static void setLog(JSONObject logs) {
        localLog.set(logs);
    }
    public static JSONObject getLog() {
        return localLog.get();
    }
    public static void clearUser() {
        localLog.remove();
    }

    @Before("log()")
    public void exBefore(JoinPoint joinPoint){
        JSONObject logs = getLog();
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        log.info("url:"+request.getRequestURL());
        log.info("uri:"+request.getRequestURI());
        log.info("method:"+request.getMethod());
        log.info("class method:"+joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName());
        String header = JSONUtil.toJsonStr(JakartaServletUtil.getHeaderMap(request));
        log.info("header-->"+header);
        String param =  JSONUtil.toJsonStr(JakartaServletUtil.getParamMap(request));
        log.info("参数param-->"+param);
        BodyReaderHttpServletRequestWrapper wrapper = new BodyReaderHttpServletRequestWrapper(request);
        String body = wrapper.getBodyString(request);
        log.info("参数body-->"+body);
        setLog(logs);
        logs = new JSONObject();
        logs.put("url",request.getRequestURL().toString());
        logs.put("uri",request.getRequestURI());
        logs.put("method",request.getMethod());
        logs.put("header",header);
        logs.put("param",param);
        logs.put("body",body);
        logs.append("id", IdUtil.fastSimpleUUID());
        mongoTemplate.save(logs,"logs");
    }

    @After("log()")
    public void exAfter(JoinPoint joinPoint){
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        log.info("class method:"+joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName()
                +"方法执行完毕！");
    }

    @AfterReturning(returning="result",pointcut="log()")
    public void exAfterReturning(Object result){
        JSONObject logs = getLog();
        if (ObjectUtil.isNull(logs) || ObjectUtil.isNull(result)){
            return;
        }

        log.info("执行返回值:{}",result);
    }

    @AfterThrowing(throwing = "ex",pointcut = "log()")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex){
        JSONObject logs = getLog();
        if (ObjectUtil.isNull(logs)){
            return;
        }
        Signature signature = joinPoint.getSignature();
        String method = signature.getName();
        log.error("执行方法{}出错,异常为{}",method,ex);
    }

}
