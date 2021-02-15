package com.whut.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.product.entity.SpuImagesEntity;

import java.util.List;
import java.util.Map;

/**
 * spu图片
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 14:01:28
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveImages(Long id, List<String> images);
}

