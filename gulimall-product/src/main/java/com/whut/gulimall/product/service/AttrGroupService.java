package com.whut.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.product.entity.AttrGroupEntity;
import com.whut.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.whut.gulimall.product.vo.SkuItemVo;
import com.whut.gulimall.product.vo.SpuItemBaseAttrVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 14:01:28
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId);

    List<SpuItemBaseAttrVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

