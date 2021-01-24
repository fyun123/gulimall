package com.whut.gulimall.member.dao;

import com.whut.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 21:12:25
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
