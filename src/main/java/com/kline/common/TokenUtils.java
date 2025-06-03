package com.kline.common;

import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import java.math.BigInteger;

public class TokenUtils {

    public static ERC20 loadTokenContract(Web3j web3j,
                                          String tokenContractAddress,
                                          Credentials credentials) {
        return ERC20.load(
                tokenContractAddress,
                web3j,
                credentials,
                new DefaultGasProvider());
    }

    // 新增带GasProvider版本
    public static ERC20 loadTokenContract(Web3j web3j,
                                          String tokenContractAddress,
                                          Credentials credentials,
                                          ContractGasProvider gasProvider) {
        return ERC20.load(
                tokenContractAddress,
                web3j,
                credentials,
                gasProvider);
    }

    public static BigInteger getTokenBalance(Web3j web3j,
                                             String tokenContractAddress,
                                             String walletAddress) throws Exception {
        TransactionManager tm = new ClientTransactionManager(web3j, null);
        ERC20 token = ERC20.load(
                tokenContractAddress,
                web3j,
                tm,
                new DefaultGasProvider());

        return token.balanceOf(walletAddress).send();
    }
}