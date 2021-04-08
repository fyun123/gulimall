package com.whut.common.to;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {
    /**
     * 库存工作单的id
     */
    private Long id;
    /**
     * 工作单详情的id
     */
    private StockDetailTo detailTo;

}
