package com.org.kline.manager;

import com.org.common.comm.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
public class PersistentHDWalletERC20Manager {

    private static final String WALLET_FILE = "/Users/Apple/Desktop/work/master_wallet.json";
    private static final String MNEMONIC_FILE = "/Users/Apple/Desktop/work/mnemonic.txt";
    private static final String ETH_DERIVATION_PATH = "m/44'/60'/0'/0/0";

    private final Web3j web3j;
    private Credentials masterCredentials;
    private DeterministicKey masterKey;
    private int nextChildIndex = 0;

    // 代币配置映射 (symbol -> contract address)
    private final Map<String, String> tokenConfigs = new HashMap<>();

    public PersistentHDWalletERC20Manager(String rpcUrl) throws Exception {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        initializeOrLoadMasterWallet();
    }

    public PersistentHDWalletERC20Manager(String rpcUrl, Map<String, String> tokenConfigs) throws Exception {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.tokenConfigs.putAll(tokenConfigs);
        initializeOrLoadMasterWallet();
    }

    /**
     * 初始化或加载主钱包
     */
    private void initializeOrLoadMasterWallet() throws Exception {
        File walletFile = new File(WALLET_FILE);
        File mnemonicFile = new File(MNEMONIC_FILE);

        if (walletFile.exists() && mnemonicFile.exists()) {
            // 从文件加载现有钱包
            loadExistingWallet(walletFile, mnemonicFile);
        } else {
            // 创建新钱包
            createNewWallet(walletFile, mnemonicFile);
        }
    }

    /**
     * 加载现有钱包
     */
    private void loadExistingWallet(File walletFile, File mnemonicFile) throws Exception {
        // 加载助记词
        String mnemonic = new String(Files.readAllBytes(mnemonicFile.toPath()), StandardCharsets.UTF_8);

        // 从助记词生成种子
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");

        // 生成主密钥
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        this.masterKey = deriveChildKeyFromPath(masterPrivateKey, ETH_DERIVATION_PATH);

        // 加载钱包文件获取子索引
        try (BufferedReader reader = new BufferedReader(new FileReader(walletFile))) {
            String line = reader.readLine();
            if (line != null) {
                this.nextChildIndex = Integer.parseInt(line.trim());
            }
        }

        // 生成主凭证
        this.masterCredentials = getCredentialsFromKey(masterKey);

        log.info("Loaded existing wallet. Master address: " + masterCredentials.getAddress());
    }

    /**
     * 根据路径派生子密钥
     */
    private DeterministicKey deriveChildKeyFromPath(DeterministicKey masterKey, String path) {
        DeterministicKey key = masterKey;
        String[] parts = path.split("/");

        for (String part : parts) {
            if (part.equals("m")) {
                continue;
            }

            boolean isHardened = part.endsWith("'");
            int childNumber;
            if (isHardened) {
                String numberStr = part.substring(0, part.length() - 1);
                childNumber = Integer.parseInt(numberStr);
                childNumber += HARDENED_BIT;
            } else {
                childNumber = Integer.parseInt(part);
            }

            key = HDKeyDerivation.deriveChildKey(key, childNumber);
        }

        return key;
    }

    private static final int HARDENED_BIT = 0x80000000;

    /**
     * 创建新钱包
     */
    private void createNewWallet(File walletFile, File mnemonicFile) throws Exception {
        // 生成随机助记词
        SecureRandom secureRandom = new SecureRandom();
        byte[] entropy = new byte[16]; // 128位熵，生成12个单词
        secureRandom.nextBytes(entropy);

        List<String> mnemonic = MnemonicCode.INSTANCE.toMnemonic(entropy);
        String mnemonicPhrase = String.join(" ", mnemonic);

        // 保存助记词到文件
        Files.write(mnemonicFile.toPath(), mnemonicPhrase.getBytes(StandardCharsets.UTF_8));

        // 从助记词生成种子
        byte[] seed = MnemonicCode.toSeed(mnemonic, "");

        // 生成主密钥
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        this.masterKey = deriveChildKeyFromPath(masterPrivateKey, ETH_DERIVATION_PATH);

        // 生成主凭证
        this.masterCredentials = getCredentialsFromKey(masterKey);

        // 保存初始钱包文件
        Files.write(walletFile.toPath(), "0".getBytes(StandardCharsets.UTF_8));

        log.info("Created new wallet. Master address: " + masterCredentials.getAddress());
        log.info("Mnemonic: " + mnemonicPhrase);
        log.info("IMPORTANT: Backup the mnemonic phrase securely!");
    }

    /**
     * 为用户生成新的子地址
     */
    public String generateNewAddress() throws IOException {
        // 派生子密钥
        DeterministicKey childKey = HDKeyDerivation.deriveChildKey(masterKey, nextChildIndex);
        Credentials childCredentials = getCredentialsFromKey(childKey);

        // 保存新的子索引
        Files.write(Paths.get(WALLET_FILE), String.valueOf(nextChildIndex + 1).getBytes(StandardCharsets.UTF_8));

        nextChildIndex++;

        return childCredentials.getAddress();
    }

    /**
     * 归集资金到主地址
     */
    public TransactionReceipt sweepFunds(String fromAddressPrivateKey, BigDecimal amount) throws Exception {
        Credentials fromCredentials = Credentials.create(fromAddressPrivateKey);

        return Transfer.sendFunds(
                web3j,
                fromCredentials,
                masterCredentials.getAddress(),
                amount,
                Convert.Unit.ETHER
        ).send();
    }

    /**
     * 归集所有子地址资金到主地址
     */
    public List<TransactionReceipt> sweepAllFunds() throws Exception {
        List<TransactionReceipt> receipts = new ArrayList<>();

        // 遍历所有已生成的子地址
        for (int i = 0; i < nextChildIndex; i++) {
            DeterministicKey childKey = HDKeyDerivation.deriveChildKey(masterKey, i);
            Credentials childCredentials = getCredentialsFromKey(childKey);

            // 获取余额
            BigInteger balance = web3j.ethGetBalance(childCredentials.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance();
            if (balance.compareTo(BigInteger.ZERO) > 0) {
                // 如果有余额，归集
                BigDecimal amount = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);

                // 需要一些ETH作为gas费，所以保留少量
                BigDecimal amountToTransfer = amount.subtract(Convert.toWei("0.001", Convert.Unit.ETHER));
                if (amountToTransfer.compareTo(BigDecimal.ZERO) > 0) {
                    TransactionReceipt receipt = sweepFunds(childCredentials.getEcKeyPair().getPrivateKey().toString(16), amountToTransfer);
                    receipts.add(receipt);
                }
            }
        }

        return receipts;
    }

    /**
     * 多币种归集
     * @param tokenSymbols 要归集的代币符号列表(如["ETH","USDT"])
     */
    public Map<String, List<TransactionReceipt>> sweepAllFunds(List<String> tokenSymbols) throws Exception {
        Map<String, List<TransactionReceipt>> allReceipts = new HashMap<>();

        for (String symbol : tokenSymbols) {
            List<TransactionReceipt> receipts = new ArrayList<>();

            if ("ETH".equalsIgnoreCase(symbol)) {
                receipts = sweepNativeToken();
            } else {
                String contractAddress = tokenConfigs.get(symbol);
                if (contractAddress != null) {
                    receipts = sweepERC20Token(contractAddress);
                }
            }

            if (!receipts.isEmpty()) {
                allReceipts.put(symbol, receipts);
            }
        }

        return allReceipts;
    }

    private List<TransactionReceipt> sweepNativeToken() throws Exception {
        List<TransactionReceipt> receipts = new ArrayList<>();

        // 动态获取当前 Gas Price（默认 5 Gwei）
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger minGasPrice = Convert.toWei("5", Convert.Unit.GWEI).toBigInteger();
        if (gasPrice.compareTo(minGasPrice) < 0) {
            gasPrice = minGasPrice; // 防止 Gas Price 过低
        }

        for (int i = 0; i < nextChildIndex; i++) {
            DeterministicKey childKey = HDKeyDerivation.deriveChildKey(masterKey, i);
            Credentials childCredentials = getCredentialsFromKey(childKey);

            BigInteger balance = web3j.ethGetBalance(
                    childCredentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).send().getBalance();

            if (balance.compareTo(BigInteger.ZERO) > 0) {
                // 计算可归集金额（保留足够 Gas）
                BigInteger estimatedGasCost = BigInteger.valueOf(21_000).multiply(gasPrice);
                BigInteger amountToTransfer = balance.subtract(estimatedGasCost);

                if (amountToTransfer.compareTo(BigInteger.ZERO) > 0) {
                    TransactionReceipt receipt = Transfer.sendFunds(
                            web3j,
                            childCredentials,
                            masterCredentials.getAddress(),
                            Convert.fromWei(amountToTransfer.toString(), Convert.Unit.ETHER),
                            Convert.Unit.ETHER
                    ).send();
                    receipts.add(receipt);
                }
            }
        }
        return receipts;
    }

    private List<TransactionReceipt> sweepERC20Token(String contractAddress) throws Exception {
        List<TransactionReceipt> receipts = new ArrayList<>();

        // 1. 固定 Gas 参数（避免异常值）
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(100_000L); // ERC20 安全值
        BigInteger estimatedGasCost = gasPrice.multiply(gasLimit); // 0.0005 ETH

        for (int i = 0; i < nextChildIndex; i++) {
            DeterministicKey childKey = HDKeyDerivation.deriveChildKey(masterKey, i);
            Credentials childCredentials = getCredentialsFromKey(childKey);
            String childAddress = childCredentials.getAddress();

            // 2. 检查代币余额
            BigInteger tokenBalance = TokenUtils.getTokenBalance(web3j, contractAddress, childAddress);
            log.info("tokenBalance: " + tokenBalance);
            //tokenBalance = tokenBalance.divide(BigInteger.TEN.pow(6)); // 转换为 USDT 单位
            //log.info("tokenBalance: " + tokenBalance);
            if (tokenBalance.compareTo(BigInteger.ZERO) <= 0) continue;

            // 3. 检查并补充 Gas 费
            BigInteger ethBalance = web3j.ethGetBalance(childAddress, DefaultBlockParameterName.LATEST).send().getBalance();
            if (ethBalance.compareTo(estimatedGasCost) < 0) {
                System.out.printf("补充 Gas 费到 %s: %s ETH%n",
                        childAddress,
                        Convert.fromWei(String.valueOf(estimatedGasCost), Convert.Unit.ETHER));
                if (!sendGasFunds(childAddress, estimatedGasCost)) {
                    log.info("主账户余额不足，无法补充 Gas");
                    continue;
                }
            }

            // 4. 执行代币转账
            StaticGasProvider gasProvider = new StaticGasProvider(gasPrice, gasLimit);
            ERC20 childToken = TokenUtils.loadTokenContract(web3j, contractAddress, childCredentials, gasProvider);
            TransactionReceipt receipt = childToken.transfer(
                    masterCredentials.getAddress(),
                    tokenBalance // 直接传递原始余额（单位已正确）
            ).send();

            receipts.add(receipt);
            System.out.printf("成功归集 %s USDT (TxHash: %s)%n",
                    new BigDecimal(tokenBalance).divide(BigDecimal.TEN.pow(6)), // 转换为可读格式
                    receipt.getTransactionHash());
        }
        return receipts;
    }
    /**
     * 从主账户发送gas费到子地址
     */
    private boolean sendGasFunds(String toAddress, BigInteger requiredAmount) throws Exception {
        BigInteger masterBalance = web3j.ethGetBalance(
                masterCredentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send().getBalance();

        // 主账户保留最少0.01 ETH
        BigInteger minReserve = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();
        BigInteger available = masterBalance.subtract(minReserve);

        if (available.compareTo(requiredAmount) >= 0) {
            Transfer.sendFunds(
                    web3j,
                    masterCredentials,
                    toAddress,
                    new BigDecimal(requiredAmount),
                    Convert.Unit.WEI
            ).send();
            return true;
        }
        return false;
    }

    /**
     * 从确定性密钥获取凭证
     */
    private Credentials getCredentialsFromKey(DeterministicKey key) {
        BigInteger privateKey = key.getPrivKey();
        ECKeyPair keyPair = ECKeyPair.create(privateKey);
        return Credentials.create(keyPair);
    }

    /**
     * 将路径字符串转换为数组
     */
    private int[] getPathAsArray(String path) {
        String[] parts = path.split("/");
        int[] pathArray = new int[parts.length - 1];

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].replace("'", "");
            pathArray[i - 1] = Integer.parseInt(part);
        }

        return pathArray;
    }

    public Credentials getMasterCredentials() {
        return masterCredentials;
    }

    /**
     * 获取主钱包地址
     */
    public String getMasterAddress() {
        return masterCredentials.getAddress();
    }
    /**
     * 获取主钱包私钥（16进制格式，不带0x前缀）
     */
    public String getMasterPrivateKey() {
        return masterKey.getPrivKey().toString(16);
    }

    public static void main(String[] args) throws Exception {
        // 配置代币合约地址
        Map<String, String> tokenConfigs = new HashMap<>();
        tokenConfigs.put("USDT", "0x8491BFaCFc2b7d7f918490aC976f3Fcc656B24F6");
        tokenConfigs.put("DAI", "0x6b175474e89094c44da98b954eedeac495271d0f");
        // 示例用法
        PersistentHDWalletERC20Manager manager = new PersistentHDWalletERC20Manager(
                "https://sepolia.infura.io/v3/57388f5634fd4ef6b9ed09b66929c1e5"
        ,tokenConfigs);

        // 为用户生成新地址
        /*String userAddress = manager.generateNewAddress();
        log.info("Generated new user address: " + userAddress);

        log.info("主钱包地址:"+manager.getMasterAddress());
        log.info("主钱包私钥:"+manager.getMasterPrivateKey());*/


        // 归集资金
        // 归集ETH和USDT
        Map<String, List<TransactionReceipt>> results = manager.sweepAllFunds(
                Arrays.asList("ETH", "USDT", "LINK")

        );
        // 打印结果
        results.forEach((symbol, receipts) -> {
            System.out.printf("%s 归集完成，共 %d 笔交易:%n", symbol, receipts.size());
            receipts.forEach(r -> log.info("  TxHash: " + r.getTransactionHash()));
        });
    }
}