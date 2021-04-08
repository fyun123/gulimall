package com.whut.gulimall.product.vo;

import com.whut.gulimall.product.entity.SkuImagesEntity;
import com.whut.gulimall.product.entity.SkuInfoEntity;
import com.whut.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;
@Data
public class SkuItemVo {
    // 1. sku基本信息获取 `pms_sku_info`
    SkuInfoEntity info;
    // 是否有货
    boolean hasStock = true;
    // 2. 获取sku的图片信息 `pms_sku_images`
    List<SkuImagesEntity> images;
    // 3. spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttrs;
    // 4. 获取spu的介绍
    SpuInfoDescEntity desc;
    // 5. 获取当前spu的规格参数信息
    List<SpuItemBaseAttrVo> baseAttrs;
//    // 6. 快速展示
//    List<SpuBaseAttrVo> quickShowAttrs;
    // 7. 秒杀信息
    SeckillInfoVo seckillInfo;

}
