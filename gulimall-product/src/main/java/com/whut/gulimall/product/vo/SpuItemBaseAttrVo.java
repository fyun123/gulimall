package com.whut.gulimall.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpuItemBaseAttrVo{
    private String groupName;
    private List<SpuBaseAttrVo> attrs;
}