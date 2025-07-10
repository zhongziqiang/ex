package com.matching.controller;

import com.matching.entity.Order;
import com.matching.service.ExchangeService;
import com.org.common.comm.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "测试")
@RestController
@RequestMapping("test")
public class TestController {

    @Autowired
    ExchangeService exchangeService;


    @Operation(summary = "测add", description = "测试add")
    @PostMapping(value = "/add")
    public Result add(@RequestBody Order order) {
        exchangeService.submitOrder(order);
        return Result.success();
    }

    @Operation(summary = "测cancel", description = "测试cancel")
    @PostMapping(value = "/cancel")
    public Result cancel(@RequestBody Order order) {
        exchangeService.cancelOrder(order);
        return Result.success();
    }

    @Operation(summary = "测update", description = "测试update")
    @PostMapping(value = "/update")
    public Result update(@RequestBody Order order) {
        exchangeService.modifyOrder(order);
        return Result.success();
    }
}
