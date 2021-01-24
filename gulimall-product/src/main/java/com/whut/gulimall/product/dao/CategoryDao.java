package com.whut.gulimall.product.dao;

import com.whut.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 14:01:28
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
