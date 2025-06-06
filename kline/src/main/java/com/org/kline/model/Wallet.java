package com.org.kline.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName wallet
 */
@TableName(value ="wallet")
@Data
public class Wallet implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 绑定的用户id,如果用户ID是0,则是主钱包
     */
    private Integer userId;

    /**
     * 钱包类型 0-默认, 1-ERC20, 2-TRC20
     */
    private Integer hdType;

    /**
     * 助记词
     */
    private String mnemonic;

    /**
     * 私钥
     */
    private String privateKey;

    /**
     * 创建人id
     */
    private Integer createId;

    /**
     * 创建人名称
     */
    private String createName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人id
     */
    private Integer updateId;

    /**
     * 更新人名称
     */
    private String updateName;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}