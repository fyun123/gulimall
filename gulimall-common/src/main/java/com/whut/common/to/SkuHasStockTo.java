package com.whut.common.to;

import lombok.Data;

@Data
public class SkuHasStockTo {
    private Long skuId;
    private Boolean hasStock;
}
