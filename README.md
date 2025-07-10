# ex 项目文档
## 项目初期本来只是想做一下k线的同步,后面突发奇想,越做越多,目前实现的功能如下:
- 1.ERC20扫链,并完成对指定币对的确认交易,如果是做交易所的同学,可以对扫链完成的数据进行上币操作
- 2.为了完成上币操作,那么就需要HD钱包,目前是基于ERC20的链,完成主账户的生成,以及对子账户地址的生成
- 3.既然有主账户和子账户,那么就需要归集操作,目前是基于ERC20的链,完成归集操作(实时获取gas费用计算最小gas费,归集的时候别忘了给主账户留一些gas费)

## 项目计划 (会陆续更新,为实现高性能,会陆续迁移到MQ,用户后续所有操作都移到缓存里面)
- 1.完成k线数据的同步
- 2.完成ERC20扫链
- 3.完成HD钱包的生成
- 4.完成归集操作
- 5.完成合约交易
- 6.完成合约撮合
- 7.完成app,web访问
- 8.完成后台管理访问
- 9.完成合约交易
- 10.完成合约撮合

## 项目结构
```
ex
├── kline  # k线数据拉取维护
├── scanner-job  # 区块扫描
├── wallet # 钱包服务,手动归集等
├── public-api # app,web访问
├── admin-api # 后端管理访问
├── futures # 合约交易(暂未实现)
├── matching-service # 合约撮合(暂未实现)
└── README.md
```


## 项目概述
GitHub 项目 [zhongziqiang/kline](https://github.com/zhongziqiang/kline) 是一个基于Java的区块链数据工具库，主要功能：

1. **K线数据拉取** - 从交易所API获取加密货币K线
2. **ERC20区块扫描** - 以太坊ERC20代币交易监听

## 技术栈
- 语言：Java 21+
- 依赖库：
    - Web3j (以太坊交互)
    - Lombok (简化代码)
    - disruptor (高性能撮合服务)

## 功能特性

### 1. K线数据获取
```java
// 支持交易所
public enum Exchange {
    BINANCE
}

// K线周期
public enum Interval {
    MIN_1, MIN_5, MIN_15,
    HOUR_1, HOUR_4,
    DAY_1, WEEK_1
}