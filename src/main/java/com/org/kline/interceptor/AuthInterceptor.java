package com.org.kline.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.org.common.comm.Result;
import com.org.kline.aspect.RequestAspect;
import com.org.kline.jwt.AuthStorage;
import com.org.kline.page.PageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Configuration
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(AuthStorage.TOKEN_KEY);
        PageContext.setPage(null);
        PageContext.init(request);
        if (StrUtil.isNotBlank(token)) {
            //TODO 记录是否需要TOEKN鉴权
            return true;
        }
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(JSONUtil.toJsonStr(Result.fail(401, "unauthorized")));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成清除授权信息
        AuthStorage.clearUser();
        RequestAspect.clearUser();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
