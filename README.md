# Kline 项目文档
## 项目初期本来只是想做一下k线的同步,后面突发奇想,越做越多,目前实现的功能如下:
- 1.ERC20扫链,并完成对指定币对的确认交易,如果是做交易所的同学,可以对扫链完成的数据进行上币操作
- 2.为了完成上币操作,那么就需要HD钱包,目前是基于ERC20的链,完成主账户的生成,以及对子账户地址的生成
- 3.既然有主账户和子账户,那么就需要归集操作,目前是基于ERC20的链,完成归集操作(实时获取gas费用计算最小gas费,归集的时候别忘了给主账户留一些gas费)

## 项目概述
GitHub 项目 [zhongziqiang/kline](https://github.com/zhongziqiang/kline) 是一个基于Java的区块链数据工具库，主要功能：

1. **K线数据拉取** - 从交易所API获取加密货币K线
2. **ERC20区块扫描** - 以太坊ERC20代币交易监听

## 技术栈
- 语言：Java 17+
- 依赖库：
    - Web3j (以太坊交互)
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