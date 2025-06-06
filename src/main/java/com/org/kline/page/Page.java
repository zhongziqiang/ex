package com.org.kline.page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Page<T> implements Serializable {

    @Schema(description = "返回的总条数")
    private int total = 0;

    @Schema(description = "返回的具体页数",defaultValue = "0")
    private int pageNum = 0;

    @Schema(description = "每页显示的条数",defaultValue = "10")
    private int pageSize = 10;


    @Schema(description = "开始页数",defaultValue = "0")
    private int startRow;

    @Schema(description = "具体返回的数据存储集合")
    private List rows = new ArrayList();

    private List footer = new ArrayList();

    public Page(int pageNum,int pageSize) {

        setPageNum(pageNum);

        setPageSize(pageSize);

        setStartRow(pageNum > 0 ? (pageNum - 1)* pageSize : 0);

    }

    public Page(){

    }
}
