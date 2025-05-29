# Kline 项目文档 (Java版)

## 项目概述
GitHub 项目 [zhongziqiang/kline](https://github.com/zhongziqiang/kline) 是一个基于Java的区块链数据工具库，主要功能：

1. **K线数据拉取** - 从交易所API获取加密货币K线
2. **ERC20区块扫描** - 以太坊ERC20代币交易监听

## 技术栈
- 语言：Java 11+
- 依赖库：
    - Web3j (以太坊交互)
    - OkHttp (HTTP请求)
    - Jackson (JSON处理)
    - Lombok (简化代码)

## 功能特性

### 1. K线数据获取
```java
// 支持交易所
public enum Exchange {
    BINANCE,
    HUOBI,
    OKEX
}

// K线周期
public enum Interval {
    MIN_1, MIN_5, MIN_15,
    HOUR_1, HOUR_4,
    DAY_1, WEEK_1
}