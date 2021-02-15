package com.whut.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.product.entity.SpuInfoDescEntity;

import java.util.Map;

/**
 * spu信息介绍
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 14:01:28
 */
public interface SpuInfoDescService extends IService<SpuInfoDescEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfoDesc(SpuInfoDescEntity spuInfoDescEntity);
}

