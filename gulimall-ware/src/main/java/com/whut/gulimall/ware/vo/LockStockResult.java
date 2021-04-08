package com.whut.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResult {

    private Long skuId;

    private Boolean locked = false;
}
