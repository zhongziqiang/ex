package com.kline.controller;

import cn.hutool.json.JSONUtil;
import com.kline.common.Result;
import com.kline.model.User;
import com.kline.service.UserService;
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

    @Operation(summary = "用户列表")
    @PostMapping(value = "/userList")
    public Result userList(@RequestBody User user) {
        log.info("user", JSONUtil.toJsonStr(user));
        return Result.success(userService.list());
    }
}
