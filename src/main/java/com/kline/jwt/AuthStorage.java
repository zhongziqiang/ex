package com.kline.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 存储本次请求的授权信息，适用于各种业务场景，包括分布式部署
 */
public class AuthStorage {

    @Schema(description = "请求头token的下标")
    public static final String TOKEN_KEY = "token";

    private static final ThreadLocal<JwtUser> JWT_USER = new ThreadLocal<JwtUser>();

    /**
     * 全局获取用户
     */
    public static JwtUser getUser() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        return JWT_USER.get();
    }

    /**
     * 设置用户
     */
    public static void setUser(String token, JwtUser user) {
        JWT_USER.set(user);
    }

    /**
     * 清除授权
     */
    public static void clearUser() {
        JWT_USER.remove();
    }
}
