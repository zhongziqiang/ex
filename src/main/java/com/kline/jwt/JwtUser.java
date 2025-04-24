package com.kline.jwt;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JwtUser {

    private boolean valid;

    private Long id;

    /**
     * 姓名
     */
    private String userName;

    /**
     * 密码
     */
    //private String password;

    /**
     * 电话
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    private Integer roleType;

    private Long pid;


    public JwtUser() {
        this.valid = false;
    }
}
