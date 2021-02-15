package com.whut.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 14:01:28
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveProAttr(List<ProductAttrValueEntity> collect);

    List<ProductAttrValueEntity> baseAttrlistforspu(Long spuId);

    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

