package com.whut.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.to.OrderTo;
import com.whut.common.to.StockLockedTo;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.ware.entity.WareSkuEntity;
import com.whut.common.to.SkuHasStockTo;
import com.whut.gulimall.ware.vo.LockStockResult;
import com.whut.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:29:26
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds);

    List<Long> orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo lockedTo);

    void unlockStock(OrderTo orderTo);
}

