package com.whut.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PurchaseItemDoneVo {

    private Long itemId;

    private Integer status;

    private String reason;
}
