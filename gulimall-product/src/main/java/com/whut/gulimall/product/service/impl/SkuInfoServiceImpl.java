package com.whut.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.whut.common.utils.R;
import com.whut.gulimall.product.entity.SkuImagesEntity;
import com.whut.gulimall.product.entity.SpuInfoDescEntity;
import com.whut.gulimall.product.feign.SeckillFeignService;
import com.whut.gulimall.product.service.*;
import com.whut.gulimall.product.vo.SeckillInfoVo;
import com.whut.gulimall.product.vo.SkuItemSaleAttrVo;
import com.whut.gulimall.product.vo.SkuItemVo;
import com.whut.gulimall.product.vo.SpuItemBaseAttrVo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.product.dao.SkuInfoDao;
import com.whut.gulimall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService saleAttrValueService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            queryWrapper.and((w)->{
                w.eq("sku_id",key).or().like("sku_name",key);
            });
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            queryWrapper.eq("catalog_id",catelogId);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            queryWrapper.eq("brand_id",brandId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)){
            queryWrapper.ge("price",min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max) ){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(new BigDecimal("0")) == 1) {
                    queryWrapper.le("price",max);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> skus = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return skus;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            // 1. sku基本信息获取 `pms_sku_info`
            SkuInfoEntity skuInfo = getById(skuId);
            skuItemVo.setInfo(skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        CompletableFuture<Void> saleFuture = infoFuture.thenAcceptAsync((res) -> {
            // 3. spu的销售属性组合
            List<SkuItemSaleAttrVo> saleAttrVos = saleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttrs(saleAttrVos);
        }, threadPoolExecutor);


        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            // 4. 获取spu的介绍 `pms_spu_info_desc`
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, threadPoolExecutor);

        CompletableFuture<Void> baseFuture = infoFuture.thenAcceptAsync((res) -> {
            // 5. 获取当前spu的规格参数信息
            List<SpuItemBaseAttrVo> attrGroupWithAttrs = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setBaseAttrs(attrGroupWithAttrs);
        }, threadPoolExecutor);

        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            // 2. 获取sku的图片信息 `pms_sku_images`
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, threadPoolExecutor);

        // 秒杀优惠信息
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSkuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckillInfoVo seckillInfoVo = r.getData("data", new TypeReference<SeckillInfoVo>() {
                });
                skuItemVo.setSeckillInfo(seckillInfoVo);
            }
        }, threadPoolExecutor);


        // 等待所有任务都完成
        CompletableFuture.allOf(saleFuture,descFuture,baseFuture,imagesFuture,seckillFuture).get();

        return skuItemVo;
    }

}