package com.whut.gulimall.order.dao;

import com.whut.gulimall.order.entity.OmsOrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 20:54:49
 */
@Mapper
public interface OmsOrderItemDao extends BaseMapper<OmsOrderItemEntity> {
	
}
