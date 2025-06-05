package com.kline.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.kline.dto.TokenConfig;
import com.kline.service.ScannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EthScannerServiceImpl implements ScannerService {

    private final Web3j web3j;
    //主网 https://mainnet.infura.io/v3/57388f5634fd4ef6b9ed09b66929c1e5
    //测试网 https://sepolia.infura.io/v3/57388f5634fd4ef6b9ed09b66929c1e5
    private String url = "https://sepolia.infura.io/v3/57388f5634fd4ef6b9ed09b66929c1e5";



    // 最后扫描的区块号
    private BigInteger lastScannedBlock = BigInteger.ZERO;
    private final Map<String, TokenConfig> tokenConfigMap = new ConcurrentHashMap<>();

    // ERC-20 Transfer事件签名
    private static final Event TRANSFER_EVENT = new Event("Transfer",
            Arrays.asList(
                    new TypeReference<Address>(true) {},  // from
                    new TypeReference<Address>(true) {},  // to
                    new TypeReference<Uint256>(false) {}  // value
            )
    );

    public EthScannerServiceImpl() {
        this.web3j = Web3j.build(new HttpService(url));
        // 初始化代币配置,可以放在缓存或者数据库中
        initTokenConfigs();
    }

    private void initTokenConfigs() {
        // 主要ERC-20代币配置
        tokenConfigMap.put("ETH_NATIVE",
                new TokenConfig("ETH", "ETH_NATIVE", 18, 12));
        tokenConfigMap.put("0xdac17f958d2ee523a2206206994597c13d831ec7",
                new TokenConfig("USDT", "0xdac17f958d2ee523a2206206994597c13d831ec7", 6, 12));
        tokenConfigMap.put("0xa0b86a33e6441d50ea7c19b6e6f8cf6d68e72f02",
                new TokenConfig("USDC", "0xa0b86a33e6441d50ea7c19b6e6f8cf6d68e72f02", 6, 12));
        tokenConfigMap.put("0x6b175474e89094c44da98b954eedeac495271d0f",
                new TokenConfig("DAI", "0x6b175474e89094c44da98b954eedeac495271d0f", 18, 12));

        //测试网 USDT代币地址
        tokenConfigMap.put("0x8491bfacfc2b7d7f918490ac976f3fcc656b24f6",
                new TokenConfig("USDT", "0x8491bfacfc2b7d7f918490ac976f3fcc656b24f6", 6, 12));
    }


    @Override
    public void scanner() {
        try {
            // 获取最新区块号
            BigInteger latestBlock = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("最新区块号: " + latestBlock);

            // 如果是第一次运行，从最新区块开始（不扫描历史区块）
            if (lastScannedBlock.equals(BigInteger.ZERO)) {
                log.info("首次运行，不扫描历史区块");
                lastScannedBlock = latestBlock;
                return;
            }

            // 计算开始扫描的区块号（上次最后区块号+1）
            BigInteger startBlock = lastScannedBlock.add(BigInteger.ONE);

            // 如果没有新区块
            if (startBlock.compareTo(latestBlock) > 0) {
                log.info("没有新区块需要扫描");
                return;
            }
            log.info("扫描区块范围: " + startBlock + " 到 " + latestBlock);

            // 扫描新区块
            for (BigInteger blockNumber = startBlock;
                 blockNumber.compareTo(latestBlock) <= 0;
                 blockNumber = blockNumber.add(BigInteger.ONE)) {

                log.info("扫描区块: " + blockNumber);
                scanBlock(blockNumber);

                // 更新最后扫描的区块号
                lastScannedBlock = blockNumber;
            }

            log.info("扫描完成，最后扫描区块号: " + lastScannedBlock);

        } catch (IOException e) {
            log.error("扫描过程中发生错误: " + e.getMessage());
        }
    }

    private void scanBlock(BigInteger blockNumber) throws IOException {
        EthBlock.Block block = web3j.ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(blockNumber), true)
                .send()
                .getBlock();

        List<EthBlock.TransactionResult> transactions = block.getTransactions();
        for (EthBlock.TransactionResult txResult : transactions) {
            EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult.get();

            // 1. 检查ETH原生转账
            if (isETHTransfer(tx)) {
                processETHTransaction(tx, blockNumber);
            }

            // 2. 检查ERC20合约调用
            if (tx.getTo() != null && tokenConfigMap.containsKey(tx.getTo().toLowerCase())) {
                processERC20Transaction(tx, blockNumber);
            }
        }
    }

    // 处理ETH原生转账
    private void processETHTransaction(EthBlock.TransactionObject tx, BigInteger blockNumber) {
        try {
            String toAddress = tx.getTo();
            if (toAddress == null) return; // 合约创建交易，跳过
            //TODO 检查接收地址是否是交易所用户地址

            // 转换ETH金额 (从wei到ETH)
            BigInteger weiAmount = tx.getValue();
            if (weiAmount.equals(BigInteger.ZERO)) {
                return; // 金额为0，跳过
            }
            BigDecimal ethAmount = Convert.fromWei(new BigDecimal(weiAmount), Convert.Unit.ETHER);
            // 检查是否已存在该交易记录

            log.info("ETH原生转账: from={}, to={}, hash={}, value={}",
                    tx.getFrom(), tx.getTo(), tx.getHash(), ethAmount);

        }catch (Exception e){
            log.error("处理ETH原生转账失败: " + e.getMessage());
        }
    }

    // 处理ERC-20交易
    private void processERC20Transaction(EthBlock.TransactionObject tx, BigInteger blockNumber) {
        try {
            // 获取交易收据，包含事件日志
            EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getHash()).send();

            if (receipt.getTransactionReceipt().isPresent()) {
                TransactionReceipt txReceipt = receipt.getTransactionReceipt().get();

                // 解析Transfer事件
                for (Log log : txReceipt.getLogs()) {
                    if (isTransferEvent(log)) {
                        processTransferEvent(log, tx, blockNumber);
                    }
                }
            }
            log.info("处理ERC-20交易: from={}, to={}, hash={}",
                    tx.getFrom(), tx.getTo(), tx.getHash());
        } catch (Exception e) {
            System.err.println("处理ERC-20交易异常: " + e.getMessage());
        }
    }

    private void processTransferEvent(Log lg, EthBlock.TransactionObject tx, BigInteger blockNumber) {
        try {
            String contractAddress = lg.getAddress().toLowerCase();
            TokenConfig tokenConfig = tokenConfigMap.get(contractAddress);

            if (tokenConfig == null || !tokenConfig.getIsActive()) {
                return;
            }

            // 解析事件参数
            List<String> topics = lg.getTopics();
            if (topics.size() < 3) return;

            String fromAddress = "0x" + topics.get(1).substring(26); // 去掉前面的0填充
            String toAddress = "0x" + topics.get(2).substring(26);
            String valueHex = lg.getData().substring(2); // 去掉0x前缀
            // 转换金额
            BigInteger rawAmount = new BigInteger(valueHex, 16);
            BigDecimal amount = new BigDecimal(rawAmount)
                    .divide(BigDecimal.TEN.pow(tokenConfig.getDecimals()));
            log.info("Transfer事件: from={}, to={}, contract={}, symbol={}, value={}",
                    fromAddress, toAddress, contractAddress, tokenConfig.getSymbol(), amount);

            // 检查接收地址是否是交易所用户地址

            // 检查是否已存在该交易记录

        }catch (Exception e){
            log.error("处理Transfer事件失败: {}", e.getMessage());
        }
    }

        private boolean isETHTransfer(EthBlock.TransactionObject tx) {
        // ETH转账的特征：
        // 1. to地址不为空（不是合约创建）
        // 2. value > 0 (有ETH转账)
        // 3. input为空或只有0x (简单转账，不是合约调用)
        return tx.getTo() != null
                && tx.getValue().compareTo(BigInteger.ZERO) > 0
                && (tx.getInput() == null || "0x".equals(tx.getInput()) || tx.getInput().isEmpty());
    }

    private boolean isTransferEvent(Log log) {
        if (log.getTopics().isEmpty()) return false;
        String eventSignature = log.getTopics().get(0);
        return EventEncoder.encode(TRANSFER_EVENT).equals(eventSignature);
    }


    //@Scheduled(fixedDelay = 10_000)
    public void scanBlocks() {
        scanner();
    }

    @Override
    public void processTransaction(Object obj) {
        EthBlock.TransactionObject tx = (EthBlock.TransactionObject) obj;
        String from = tx.getFrom();
        String to = tx.getTo();
        String txHash = tx.getHash();
        BigInteger value = tx.getValue();
        log.info("Transaction: from={}, to={}, hash={}, value={}", from, to, txHash, convertWeiToEther(value));
        // 实现处理交易逻辑
        log.info("处理交易...");
    }

    private static String convertWeiToEther(BigInteger wei) {
        //NumberUtil.div(wei, new BigInteger("1000000000000000000")).divide(new BigDecimal("100")).toString();
        //return wei.divide(new BigInteger("10000000000000000")).doubleValue() / 100.0 + "";
        BigDecimal value = NumberUtil.div(wei, new BigInteger("1000000000000000000")).divide(new BigDecimal("100"));
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            value = BigDecimal.ZERO;
        }
        return value.toString();
    }
}
