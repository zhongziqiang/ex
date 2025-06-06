package com.org.kline.dto;

import lombok.Data;

@Data
public class TokenConfig {
    private Long id;

    private String symbol; // USDT, USDC, DAI等

    private String contractAddress;

    private Integer decimals;

    private Integer minConfirmBlocks;

    private Boolean isActive;

    public TokenConfig(String symbol, String contractAddress, Integer decimals, Integer minConfirmBlocks) {
        this.symbol = symbol;
        this.contractAddress = contractAddress;
        this.decimals = decimals;
        this.minConfirmBlocks = minConfirmBlocks;
        this.isActive = true;
    }
}
