package com.whut.gulimall.coupon.dao;

import com.whut.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:05:59
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
