package com.whut.gulimall.ware.dao;

import com.whut.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:29:26
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}
