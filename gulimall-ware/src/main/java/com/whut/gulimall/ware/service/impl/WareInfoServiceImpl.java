package com.whut.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.whut.common.utils.R;
import com.whut.gulimall.ware.feign.MemberFeignService;
import com.whut.gulimall.ware.vo.FareVo;
import com.whut.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.ware.dao.WareInfoDao;
import com.whut.gulimall.ware.entity.WareInfoEntity;
import com.whut.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(key)){
            queryWrapper.eq("id",key)
                    .or().like("name",key)
                    .or().like("address",key)
                    .or().like("areacode",key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo address = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() { });
        fareVo.setAddress(address);
        if (address != null){
            // 接入快递接口
            String phone = address.getPhone();
            String substring = phone.substring(phone.length() - 1);
            fareVo.setFare(new BigDecimal(substring));
        }
        return fareVo;
    }

}