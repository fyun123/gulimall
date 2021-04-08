package com.whut.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能
 */
@Data
public class SearchParam {

    private String keyword; //全文匹配关键字

    private Long catalog3Id;//三级分类id

    /**
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc_desc
     * sort=hotScore_asc_desc
     */
    private String sort;//排序条件

    /**
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * brandId=1
     * attrs=1_其他:安卓
     */
    private Integer hasStock; // 是否有库存

    private String skuPrice; // 价格区间

    private List<Long> brandId; // 按照品牌进行查询，可以多选

    private List<String> attrs; // 按照属性进行筛选

    private Integer pageNum = 1; //页码

    private String queryString; // 原生的所有查询条件
}
