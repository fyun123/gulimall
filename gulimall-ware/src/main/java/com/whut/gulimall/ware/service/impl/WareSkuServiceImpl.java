package com.whut.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.whut.common.exception.NoStockException;
import com.whut.common.to.OrderTo;
import com.whut.common.to.StockDetailTo;
import com.whut.common.to.StockLockedTo;
import com.whut.common.utils.R;
import com.whut.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.whut.gulimall.ware.entity.WareOrderTaskEntity;
import com.whut.gulimall.ware.feign.OrderFeignService;
import com.whut.gulimall.ware.feign.ProductFeignService;
import com.whut.common.to.SkuHasStockTo;
import com.whut.gulimall.ware.service.WareOrderTaskDetailService;
import com.whut.gulimall.ware.service.WareOrderTaskService;
import com.whut.gulimall.ware.vo.LockStockResult;
import com.whut.gulimall.ware.vo.OrderItemVo;
import com.whut.gulimall.ware.vo.OrderVo;
import com.whut.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.ware.dao.WareSkuDao;
import com.whut.gulimall.ware.entity.WareSkuEntity;
import com.whut.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Resource
    private WareSkuDao wareSkuDao;

    @Resource
    private ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService taskService;

    @Autowired
    WareOrderTaskDetailService taskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 1. 如果没有库存记录，，需要新增
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // TODO 发生异常不回滚方法
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {

            }
            wareSkuDao.insert(wareSkuEntity);
        }
        wareSkuDao.addStock(skuId, wareId, skuNum);
    }

    @Override
    public List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockTo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockTo stockTo = new SkuHasStockTo();
            //查询当前sku库存量
            //select SUM(stock-stock_locked) from `wms_ware_sku` where sku_id=?
            Long count = baseMapper.getSkuStock(skuId);
            stockTo.setSkuId(skuId);
            stockTo.setHasStock(count != null && count > 0);
            return stockTo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为某个订单锁定库存
     *
     * @param vo
     * @return
     */
    @Transactional
    @Override
    public List<Long> orderLockStock(WareSkuLockVo vo) {
        // 保存库存工作单的详情
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        taskService.save(taskEntity);

        List<Long> failSkuIds = new ArrayList<>();
        // 找到哪些仓库商品有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());
        // 锁定库存
        for (SkuWareHasStock hasStock : collect) {
            boolean flag = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                failSkuIds.add(skuId);
            } else {
                for (Long wareId : wareIds) {
                    Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                    if (count == 1) {
                        // 锁成功了
                        flag = true;
                        // 告诉RabbitMq库存锁定成功
                        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
                        taskDetailService.save(taskDetailEntity);
                        StockLockedTo stockLockedTo = new StockLockedTo();
                        stockLockedTo.setId(taskEntity.getId());
                        // 防止回滚后找不到数据
                        StockDetailTo stockDetailTo = new StockDetailTo();
                        BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                        stockLockedTo.setDetailTo(stockDetailTo);
                        rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                        break;
                    }
                }
                if (!flag) {
                    failSkuIds.add(skuId);
                }
            }
        }
        if (failSkuIds != null && failSkuIds.size() > 0) {
            throw new NoStockException(failSkuIds);
        }
        return null;
    }

    @Override
    public void unlockStock(StockLockedTo lockedTo) {
        // 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。但是远程调用库存服务，锁定的库存没法自动回滚，需手动解锁
        StockDetailTo detailTo = lockedTo.getDetailTo();
        Long detailId = detailTo.getId();
        // 解锁
        // 先去数据库查询是否有工作单详情信息
        WareOrderTaskDetailEntity detail = taskDetailService.getById(detailId);
        if (detail != null) {
            // 订单状态：已取消，需解锁
            Long id = lockedTo.getId();
            WareOrderTaskEntity taskEntity = taskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            // 远程调用订单服务，查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo orderVo = r.getData("data", new TypeReference<OrderVo>() {
                });
                if (orderVo == null || orderVo.getStatus() == 4) {
                    // 订单不存在或者订单已取消，都需要解锁库存
                    if (detail.getLockStatus() == 1) {
                        unlockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detail.getId());
                        System.out.println("库存已解锁");
                    }
                }
            } else {
                // 消息拒绝，重新放回队列
                throw new RuntimeException("远程服务失败");
            }
        }
        // 锁定库存抛出异常，已经回滚，无需解锁

    }

    // 防止订单服务卡顿，导致订单状态消息一直改不了，库存消息优先到期，查询订单状态为新建状态，不解锁库存，并删除消息，导致库永远无法解锁
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 无需查询订单最新状态，能来到这，肯定更新了订单状态的
//        R r = orderFeignService.getOrderStatus(orderSn);
        WareOrderTaskEntity task = taskService.getTaskByOrderSn(orderSn);
        if (task != null) {
            List<WareOrderTaskDetailEntity> taskDetails = taskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                    .eq("task_id", task.getId())
                    .eq("lock_status", 1));
            if (taskDetails != null && taskDetails.size() > 0) {
                for (WareOrderTaskDetailEntity taskDetail : taskDetails) {
                    unlockStock(taskDetail.getSkuId(), taskDetail.getWareId(), taskDetail.getSkuNum(), taskDetail.getId());
                    System.out.println("库存已解锁");
                }
            }

        }
    }


    private void unlockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 库存解锁
        wareSkuDao.unlockStock(skuId, wareId, num);
        // 更新工作单状态
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(taskDetailId);
        detailEntity.setLockStatus(2);
        taskDetailService.updateById(detailEntity);
    }

    @Data
    static class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}