package com.kline.service.impl;

import cn.hutool.core.util.HexUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.kline.dto.TokenConfig;
import com.kline.service.ScannerService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.java_websocket.protocols.Protocol;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.Constant;
import org.tron.trident.core.utils.Base58;
import org.tron.trident.core.utils.ByteArray;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TronScannerServiceImpl implements ScannerService {

    private ApiWrapper apiWrapper;

    private final String privateKey = "sadsfsgd";

    // 最后扫描的区块号, 可以放在缓存或者数据库中
    private long lastScannedBlock = 0L;

    private final Map<String, TokenConfig> tokenConfigMap = new ConcurrentHashMap<>();

    private void initTokenConfigs() {
        // 主要TRC-20代币配置
        tokenConfigMap.put("TRX_NATIVE",
                new TokenConfig("TRX", "TRX_NATIVE", 18, 12));
        tokenConfigMap.put("tr7nhqjekqxgtci8q8zy4pl8otszgjlj6t",
                new TokenConfig("USDT", "tr7nhqjekqxgtci8q8zy4pl8otszgjlj6t", 6, 12));
        tokenConfigMap.put("tekxitehnzsmse2xqrbj4w32run966rdz8",
                new TokenConfig("USDC", "tekxitehnzsmse2xqrbj4w32run966rdz8", 6, 12));

        //测试网 自己发布的 USDT代币地址
        tokenConfigMap.put("tlac1qjgvtggwc8evtu83wmmy9s2pdg3ny",
                new TokenConfig("USDT", "tlac1qjgvtggwc8evtu83wmmy9s2pdg3ny", 6, 12));
    }

    public TronScannerServiceImpl() {
        initTokenConfigs();
        // 连接主网或者测试网,测试网不需要私钥
        this.apiWrapper = new ApiWrapper(
                Constant.TRONGRID_SHASTA,
                Constant.TRONGRID_SHASTA_SOLIDITY,
                "770aaf4cf67446036bcf299562895fdee491ca7b833cc2db3af9cdd414907788"  //
        );
    }

    @Override
    public void scanner() {
        try {
            // 获取最新区块号
            long latestBlock = apiWrapper.getNowBlock().getBlockHeader().getRawData().getNumber();
            log.info("最新区块号: " + latestBlock);  // 打印最新区块号
            // 如果是第一次运行，从最新区块开始（不扫描历史区块）
            if (lastScannedBlock == 0) {
                log.info("首次运行，不扫描历史区块");
                lastScannedBlock = latestBlock;
                return;
            }
            // 计算开始扫描的区块号（上次最后区块号+1）
            long startBlock = lastScannedBlock + 1;
            // 如果没有新区块
            if (startBlock > latestBlock) {
                log.info("没有新区块需要扫描");
                return;
            }
            log.info("扫描区块范围: " + startBlock + " 到 " + latestBlock);

            // 扫描区块
            for (long blockNum = startBlock; blockNum <= latestBlock; blockNum++) {
                log.info("正在扫描区块: " + blockNum);
                // 获取区块信息
                Response.BlockExtention block = apiWrapper.getBlockByNum(blockNum);
                if (block != null) {
                    // 处理区块中的交易
                    block.getTransactionsList().forEach(tx -> {
                        // 处理交易
                        String txId = HexUtil.encodeHexStr(tx.getTxid().toByteArray());
                        log.info("交易ID: " + txId);  // 打印交易ID
                        analyzeTransaction(tx);

                    });
                }
            }
            // 更新最后扫描的区块号
            lastScannedBlock = latestBlock;
        }catch (Exception e){
            log.error("扫描过程中发生错误: " + e.getMessage());
        }
    }

    private void analyzeTransaction(Response.TransactionExtention txExt) {
        // 遍历合约列表
        for (Chain.Transaction.Contract contract : txExt.getTransaction().getRawData().getContractList()) {
            switch (contract.getType()) {
                case TransferContract:
                    parseTransferContract(contract);
                    break;
                case TriggerSmartContract:
                    parseTriggerSmartContract(contract);
                    break;
                // 其他合约类型...
            }
        }
    }

    // 普通转账
    private void parseTransferContract(Chain.Transaction.Contract contract) {
        try {
            Contract.TransferContract transferContract = contract.getParameter().unpack(Contract.TransferContract.class);
            String from = Base58Check.bytesToBase58(transferContract.getOwnerAddress().toByteArray());
            String to = Base58Check.bytesToBase58(transferContract.getToAddress().toByteArray());
            long amountong = transferContract.getAmount();
            BigDecimal amount = new BigDecimal(amountong).divide(new BigDecimal(1000000));

            log.info("普通转账: from=" + from + ", to=" + to + ", amount=" + amount);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    // 代币转账
    private void parseTriggerSmartContract(Chain.Transaction.Contract contract) {
        try {
            Contract.TriggerSmartContract trigger = contract.getParameter().unpack(Contract.TriggerSmartContract.class);
            String contractAddress = Base58Check.bytesToBase58(trigger.getContractAddress().toByteArray());
            byte[] data = trigger.getData().toByteArray();
            // TRC20代币转账函数签名是 "a9059cbb" (transfer方法)
            String dataHex = ByteArray.toHexString(data);

            if (dataHex.startsWith("a9059cbb")) {
                // 解析转账目标地址和金额
                String toAddressHex = dataHex.substring(8, 8 + 64);
                String amountHex = dataHex.substring(8 + 64, 8 + 64 + 64);

                String toAddressBase58 = parseAddressFromHex(toAddressHex);
                long tokenAmount = new BigInteger(amountHex, 16).longValue();

                String fromAddress = Base58Check.bytesToBase58(trigger.getOwnerAddress().toByteArray());
                log.info("代币转账: contractAddress ="+ contractAddress +",  from=" + fromAddress + ", to=" + toAddressBase58 + ", amount=" + tokenAmount);

                //TODO 处理自定义币对信息,例如精度等
                TokenConfig tokenConfig = tokenConfigMap.get(contractAddress.toLowerCase());
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private String parseAddressFromHex(String hex) {
        // hex 是64字符16进制字符串，实际地址是后40字符（20字节）
        String addressHex = hex.substring(24); // 跳过前12字节（24个16进制字符）
        byte[] addressBytes = ByteArray.fromHexString(addressHex);

        // 新建一个21字节数组，首字节为0x41（TRON地址版本），后面跟20字节地址
        byte[] tronAddressBytes = new byte[21];
        tronAddressBytes[0] = 0x41;
        System.arraycopy(addressBytes, 0, tronAddressBytes, 1, 20);

        // Base58Check编码得到TRON地址
        return Base58Check.bytesToBase58(tronAddressBytes);
    }

    @Override
    public void processTransaction(Object obj) {

    }

    @Scheduled(fixedDelay = 10_000)
    public void scanBlocks() {
        synchronized(this) {
            scanner();
        }
    }
}
