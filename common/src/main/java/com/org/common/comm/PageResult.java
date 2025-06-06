package com.org.common.comm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页返回结果
 * @param <T> 数据类型
 */
@Data
@Schema(description = "分页返回结果")
public class PageResult<T> {
    @Schema(description = "当前页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "总页数", example = "10")
    private Integer pages;

    @Schema(description = "数据列表")
    private List<T> list;

    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages((int) Math.ceil((double) total / pageSize));
        result.setList(list);
        return result;
    }
}