package com.whut.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.whut.common.utils.R;
import com.whut.gulimall.order.feign.ProductFeignService;
import com.whut.gulimall.order.vo.OrderItemVo;
import com.whut.gulimall.order.vo.SpuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.order.dao.OmsOrderItemDao;
import com.whut.gulimall.order.entity.OmsOrderItemEntity;
import com.whut.gulimall.order.service.OmsOrderItemService;


@Service("omsOrderItemService")
public class OmsOrderItemServiceImpl extends ServiceImpl<OmsOrderItemDao, OmsOrderItemEntity> implements OmsOrderItemService {

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OmsOrderItemEntity> page = this.page(
                new Query<OmsOrderItemEntity>().getPage(params),
                new QueryWrapper<OmsOrderItemEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public OrderItemVo getItemByOrderSn(String orderSn) {
        OmsOrderItemEntity order = this.getOne(new QueryWrapper<OmsOrderItemEntity>().eq("order_sn", orderSn));
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setSkuId(order.getSkuId());
        orderItemVo.setTitle(order.getSkuName());
        orderItemVo.setCount(order.getSkuQuantity());
        orderItemVo.setImage(order.getSkuPic());
        orderItemVo.setPrice(order.getRealAmount());
        R r = productFeignService.getSpuInfoById(order.getSpuId());
        if (r.getCode() == 0){
            SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {
            });
            orderItemVo.setWeight(spuInfo.getWeight());
        }
        orderItemVo.setTotalPrice(order.getRealAmount());
        List<String> list = new ArrayList<>();
        list.add(order.getSkuAttrsVals());
        orderItemVo.setSkuAttr(list);
        System.out.println(orderItemVo);
        return orderItemVo;
    }

}