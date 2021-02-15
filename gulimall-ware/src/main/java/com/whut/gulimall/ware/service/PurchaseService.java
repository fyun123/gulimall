package com.whut.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.ware.entity.PurchaseEntity;
import com.whut.gulimall.ware.vo.MergeVo;
import com.whut.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:29:26
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceivePurchase(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void received(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

