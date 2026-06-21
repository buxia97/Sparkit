package com.sparkit.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用分页查询参数
 */
@Data
@NoArgsConstructor
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private long page = 1;
    private long pageSize = 10;
    private String sortField;
    private String sortOrder;
    private String keyword;
    private String startTime;
    private String endTime;

    public long getPageSize() {
        return Math.min(pageSize, 100);
    }
}