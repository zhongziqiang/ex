package com.org.kline.jwt;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * token管理
 */
@Slf4j
//@ApiModel("token提供者")
@Component
public class TokenProvider {
    @Schema(description = "盐")
    private static final String SALT_KEY = "saas-merchent";

    @Schema(description = "令牌有效期毫秒")
    private static final long TOKEN_VALIDITY = 86400000;
    //private static final long TOKEN_VALIDITY = 60000;

    @Schema(description = "有效偏差毫秒")
    private static final long Allowed_Clock_Skew_Seconds = 604800000;

    @Schema(description = "权限密钥")
    private static final String AUTHORITIES_KEY = "auth";

    @Schema(description = "Base64 密钥")
    private final static byte[] SECRET_KEY = SALT_KEY.getBytes();

    @Schema(description = "加签器")
    private final static JWTSigner jwtSigner = JWTSignerUtil.hs256(SECRET_KEY);

    private static TokenProvider tokenProvider;

    @PostConstruct
    public void init(){
        tokenProvider = this;
    }

    /**
     * 生成token
     * @param userId 用户id
     * @param clientId 用于区别客户端，如移动端，网页端，此处可根据自己业务自定义
     * @param role 角色权限
     */
    public static String createToken(String userId, String clientId, String role,JwtUser jwtUser) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + TOKEN_VALIDITY);
        Map map = new HashMap();
        //标准载荷信息
        map.put(JWTPayload.ISSUED_AT,now); //签发时间
        map.put(JWTPayload.EXPIRES_AT,validity); //过期时间
        map.put(JWTPayload.NOT_BEFORE,now); //生效时间
        map.put(JWTPayload.AUDIENCE,clientId); //代表这个JWT的接收对象
        map.put(JWTPayload.SUBJECT,userId);
        //用户载荷信息
        map.putAll( BeanUtil.beanToMap(jwtUser) );
        //你使用的JWTUtil.createToken()默认会使用HMacJWTSigner(默认算法是HmacSHA256)作为加签器,
        // 而JWTUtil.parseToken()得到的JWT, 你没有设置signer参数, 此时默认使用NoneJWTSigner作为加签器,
        // 所以校验结果是false.所以, 你在createToken时指定一个加签器, parseToken时也指定相同加签器即可
        String token = JWTUtil.createToken(map,SECRET_KEY);

        return token;
    }

    /**
     * 校验token
     */
    public static JwtUser checkToken(String token) {
        if (validateToken(token)) {
            JWT jwt = JWTUtil.parseToken(token);
            JwtUser jwtUser = jwt.getPayloads().toBean(JwtUser.class);
            Object audience = jwt.getPayload(JWTPayload.AUDIENCE);
            jwtUser.setValid(true);
            log.info("===token有效{},客户端{}", jwtUser, audience);
            return jwtUser;
        }
        log.error("***token无效***");
        return new JwtUser();
    }


    private static boolean validateToken(String authToken) {
        try {
            boolean b = JWTUtil.verify(authToken, SECRET_KEY);
            log.info("authToken:{}",authToken);
            log.info("verify结果:{}",b);
            if (b) { //验证是否过期
                JWT jwt = JWTUtil.parseToken(authToken);
                //保证加签器是和签名时的一样
                //jwt.setSigner(jwtSigner);
                long exp = Long.valueOf(jwt.getPayload("exp").toString()); //过期时间
                long now = DateUtil.currentSeconds();
                if (now > exp){ //当前时间大于过期时间
                    return false;
                }
                //b = jwt.validate( 0 );
                //log.info("validate结果:{}",b);
               return b;
            }
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("无效的token：" + authToken);
        }
        return false;
    }

    public static JWT getTokenBody(String token) {
        return JWTUtil.parseToken(token);
    }

}
