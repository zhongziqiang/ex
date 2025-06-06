package com.org.kline.controller;

import com.org.common.comm.Result;
import com.org.kline.model.User;
import com.org.kline.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "用户信息")
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户注册", description = "用户注册接口")
    @PostMapping(value = "/userList")
    public Result userList(@RequestBody User user) {
        //测试,随机生成一条用户信息

        return Result.success();
    }
}
