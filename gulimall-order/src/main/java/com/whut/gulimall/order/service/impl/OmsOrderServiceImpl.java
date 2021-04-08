package com.whut.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.whut.common.exception.NoStockException;
import com.whut.common.to.OrderTo;
import com.whut.common.to.SeckillOrderTo;
import com.whut.common.utils.R;
import com.whut.common.vo.MemberResVo;
import com.whut.gulimall.order.constant.OrderConstant;
import com.whut.gulimall.order.entity.OmsOrderItemEntity;
import com.whut.gulimall.order.entity.OmsPaymentInfoEntity;
import com.whut.gulimall.order.enume.OrderStatusEnum;
import com.whut.gulimall.order.feign.CartFeignService;
import com.whut.gulimall.order.feign.MemberFeignService;
import com.whut.gulimall.order.feign.ProductFeignService;
import com.whut.gulimall.order.feign.WareFeignService;
import com.whut.gulimall.order.interceptor.LoginUserInterceptor;
import com.whut.gulimall.order.service.OmsOrderItemService;
import com.whut.gulimall.order.service.OmsPaymentInfoService;
import com.whut.gulimall.order.to.OrderCreateTo;
import com.whut.gulimall.order.vo.*;
//import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.order.dao.OmsOrderDao;
import com.whut.gulimall.order.entity.OmsOrderEntity;
import com.whut.gulimall.order.service.OmsOrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("omsOrderService")
public class OmsOrderServiceImpl extends ServiceImpl<OmsOrderDao, OmsOrderEntity> implements OmsOrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OmsOrderItemService orderItemService;

    @Autowired
    OmsPaymentInfoService paymentInfoService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OmsOrderEntity> page = this.page(
                new Query<OmsOrderEntity>().getPage(params),
                new QueryWrapper<OmsOrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        // 获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都要共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1. 远程查询所有的收货人地址
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResVo.getId());
            orderConfirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> getCartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2. 远程查询购物车中选中的购物项
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
        }, executor).thenRunAsync(()->{
            // 远程查询是否有库存
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getSkuHasStock(collect);
            List<SkuStockVo> skuStocks = r.getData("data", new TypeReference<List<SkuStockVo>>() {
            });
            if (skuStocks != null){
                Map<Long, Boolean> stocks = skuStocks.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                orderConfirmVo.setStocks(stocks);
            }
        },executor);


        // 3. 查询用户积分
        Integer integration = memberResVo.getIntegration();
        orderConfirmVo.setIntegration(integration);
        // 4. 其他数据自动计算
        // 5. 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberResVo.getId(),token,30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);
        CompletableFuture.allOf(getAddressFuture,getCartFuture).get();
        return orderConfirmVo;
    }

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        orderSubmitVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);
        // 0 - 校验失败 1 - 删除成功
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        // 原子验证令牌和删除令牌
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResVo.getId()), orderToken);
        String s = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX+ memberResVo.getId());
        if (execute == 0L){
            // 令牌校验失败
            responseVo.setCode(1);
        } else {
            // 令牌校验成功
            // 1. 创建订单
            OrderCreateTo order = createOrder(vo);
            // 2. 验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue())< 0.01){
                // 保存订单
                saveOrder(order);
                // 锁定库存
                // 订单号、订单项（skuId,skuName,num)
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> collect = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(collect);
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0){
                    // 锁成功
                    responseVo.setOrder(order.getOrder());
//                    int i=10/0;
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());
                } else {
                    List<Long> skuIds = r.getData("skuIds", new TypeReference<List<Long>>() {
                    });
//                    responseVo.setFailSkuIds(skuIds);
//                    // 锁失败
//                    responseVo.setCode(3);
                    throw new  NoStockException(skuIds);
                }

            } else {
                responseVo.setCode(2); //金额对比失败
            }
        }
        return responseVo;
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OmsOrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        payVo.setOut_trade_no(orderSn);
        BigDecimal bigDecimal = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        List<OmsOrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OmsOrderItemEntity>().eq("order_sn", orderSn));
        OmsOrderItemEntity orderItemEntity = order_sn.get(0);
        payVo.setSubject(orderItemEntity.getSkuName());
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItems(Map<String, Object> params) {
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        IPage<OmsOrderEntity> page = this.page(
                new Query<OmsOrderEntity>().getPage(params),
                new QueryWrapper<OmsOrderEntity>().eq("member_id",memberResVo.getId()).orderByDesc("id")
        );
        List<OmsOrderEntity> order_sn = page.getRecords().stream().map(order -> {
            List<OmsOrderItemEntity> items = orderItemService.list(new QueryWrapper<OmsOrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItemEntities(items);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(order_sn);
        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1. 保存交易流水
        OmsPaymentInfoEntity paymentInfoEntity = new OmsPaymentInfoEntity();
        paymentInfoEntity.setOrderSn(vo.getOut_trade_no());
        paymentInfoEntity.setAlipayTradeNo(vo.getTrade_no());
        paymentInfoEntity.setTotalAmount(new BigDecimal(vo.getTotal_amount()));
        paymentInfoEntity.setSubject(vo.getSubject());
        paymentInfoEntity.setPaymentStatus(vo.getTrade_status());
        paymentInfoEntity.setCreateTime(vo.getGmt_create());
        paymentInfoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(paymentInfoEntity);
        // 修改订单的状态信息
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")){
            // 支付成功状态
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    @Override
    public OmsOrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OmsOrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OmsOrderEntity orderEntity) {
        // 查询当前订单是否付款
        OmsOrderEntity order = this.getById(orderEntity.getId());
        if (order.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()){
            OmsOrderEntity updateOrder = new OmsOrderEntity();
            updateOrder.setId(order.getId());
            updateOrder.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateOrder);
            // 发送MQ
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order,orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
        }
    }

    @Transactional
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {

        // 保存订单项信息
        Long skuId = seckillOrder.getSkuId();
        OmsOrderItemEntity orderItem = new OmsOrderItemEntity();
        orderItem.setOrderSn(seckillOrder.getOrderSn()); //订单号
        orderItem.setSkuId(skuId); //skuId
        R skuR = productFeignService.getSkuInfo(skuId);
        if (skuR.getCode() == 0){
            SkuInfoVo skuInfo = skuR.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });
            orderItem.setSkuName(skuInfo.getSkuTitle()); //skuName
            orderItem.setSkuPic(skuInfo.getSkuDefaultImg()); //skuPic
            orderItem.setSkuPrice(skuInfo.getPrice()); //skuPrice
            // 促销金额
            orderItem.setPromotionAmount((skuInfo.getPrice().subtract(seckillOrder.getSeckillPrice())).multiply(new BigDecimal(seckillOrder.getNum())));
            // 设置sku销售属性
            List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
            orderItem.setSkuAttrsVals(StringUtils.collectionToCommaDelimitedString(skuSaleAttrValues));
        }
        orderItem.setSkuQuantity(seckillOrder.getNum()); //skuQuantity
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        if (r.getCode() == 0){
            SpuInfoVo spuInfo = r.getData("data", new TypeReference<SpuInfoVo>() {
            });
            orderItem.setSpuId(spuInfo.getId()); //spuId
            orderItem.setSpuName(spuInfo.getSpuName()); //spuName
            R brandInfo = productFeignService.getBrandInfo(spuInfo.getBrandId());
            if (brandInfo.getCode() == 0){
                BrandVo brand = brandInfo.getData("brand", new TypeReference<BrandVo>() {
                });
                orderItem.setSpuBrand(brand.getName()); // spuBrand
            }
            orderItem.setCategoryId(spuInfo.getCatalogId()); //catalogId
            // 3. 商品的优惠信息

            // 优惠券优惠金额
            orderItem.setCouponAmount(new BigDecimal("0"));
            // 积分优惠金额
            orderItem.setIntegrationAmount(new BigDecimal("0"));
            // 优惠后的最终金额
            BigDecimal total = orderItem.getSkuPrice().multiply(new BigDecimal(seckillOrder.getNum().toString()));
            orderItem.setRealAmount(total
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getIntegrationAmount()));
            // 4. 积分信息
            orderItem.setGiftGrowth(seckillOrder.getSeckillPrice().multiply(new BigDecimal(seckillOrder.getNum().toString())).intValue());
            orderItem.setGiftIntegration(seckillOrder.getSeckillPrice().multiply(new BigDecimal(seckillOrder.getNum().toString())).intValue());
            orderItemService.save(orderItem);
            // 保存订单信息
            OmsOrderEntity order = new OmsOrderEntity();
            order.setMemberId(seckillOrder.getMemberId());
            order.setOrderSn(seckillOrder.getOrderSn());
            order.setCreateTime(new Date());

            order.setTotalAmount(orderItem.getRealAmount());

            order.setPromotionAmount(orderItem.getPromotionAmount());
            order.setIntegrationAmount(orderItem.getIntegrationAmount());
            order.setCouponAmount(orderItem.getCouponAmount());

            order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
            order.setAutoConfirmDay(7);
            order.setIntegration(orderItem.getGiftIntegration());
            order.setGrowth(orderItem.getGiftGrowth());
            order.setModifyTime(new Date());
            this.save(order);
            rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order);
        }
    }

    @Override
    public OrderConfirmVo toSeckillConfirm(String orderSn) throws ExecutionException, InterruptedException {

        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        // 获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        // 1. 查询收获地址
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都要共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1. 远程查询所有的收货人地址
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResVo.getId());
            orderConfirmVo.setAddress(address);
        }, executor);
        // 2. 设置购物项
        OrderItemVo orderItemVo = orderItemService.getItemByOrderSn(orderSn);
        ArrayList<OrderItemVo> orderItemVos = new ArrayList<>();
        orderItemVos.add(orderItemVo);
        orderConfirmVo.setItems(orderItemVos);
        // 3. 有库存
        Map<Long, Boolean> map = new HashMap<>();
        map.put(orderItemVo.getSkuId(),true);
        orderConfirmVo.setStocks(map);
        // 4. 查询用户积分
        Integer integration = memberResVo.getIntegration();
        orderConfirmVo.setIntegration(integration);

        // 5. 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberResVo.getId(),token,30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);
        CompletableFuture.allOf(getAddressFuture).get();
        return orderConfirmVo;
    }

    @Override
    public SubmitOrderResponseVo submitSeckillOrder(String orderSn, OrderSubmitVo vo) {
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        OmsOrderEntity order = getOrderByOrderSn(orderSn);
        // 2. 获取收获地址信息
        Long id = order.getId();
        OmsOrderEntity orderEntity = new OmsOrderEntity();
        orderEntity.setId(id);
        orderEntity.setMemberUsername(memberResVo.getUsername());
        R fare = wareFeignService.getFare(vo.getAddrId());
        if (fare.getCode() == 0){
            OrderFareVo fareResp = fare.getData("data", new TypeReference<OrderFareVo>() {
            });
            // 2.1 设置运费信息
            orderEntity.setFreightAmount(fareResp.getFare());
            // 2.2 设置收货地址信息
            //String receiverName;String receiverPhone;String receiverPostCode;
            // String receiverProvince;String receiverCity; String receiverRegion;
            // String receiverDetailAddress;
            orderEntity.setReceiverName(fareResp.getAddress().getName());
            orderEntity.setReceiverPhone(fareResp.getAddress().getPhone());
            orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareResp.getAddress().getProvince());
            orderEntity.setReceiverCity(fareResp.getAddress().getCity());
            orderEntity.setReceiverRegion(fareResp.getAddress().getRegion());
            orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        }
        orderEntity.setPayAmount(order.getTotalAmount().add(orderEntity.getFreightAmount()));
        this.updateById(orderEntity);
        responseVo.setOrder(getOrderByOrderSn(orderSn));
        responseVo.setCode(0);
        responseVo.setFailSkuIds(null);
        System.out.println("提交响应:"+responseVo);
        return responseVo;
    }

    private void saveOrder(OrderCreateTo order){
        OmsOrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OmsOrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(OrderSubmitVo vo){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 1. 生成订单号
        String orderSn = IdWorker.getTimeId();
        OmsOrderEntity order = buildOrder(vo, orderSn);
        // 2. 获取所有订单项数据
        List<OmsOrderItemEntity> orderItems = buildOrderItems(orderSn);
        // 3. 验价
        if (orderItems != null && orderItems.size() > 0){
            computePrice(order, orderItems);
        }
        orderCreateTo.setOrder(order);
        orderCreateTo.setOrderItems(orderItems);
        return orderCreateTo;
    }

    private void computePrice(OmsOrderEntity order, List<OmsOrderItemEntity> orderItems) {
        // 订单总额
        BigDecimal totalPrice = new BigDecimal("0.00");
        // 促销优惠
        BigDecimal totalPromotion= new BigDecimal("0.00");
        // 积分优惠
        BigDecimal totalIntegration = new BigDecimal("0.00");
        // 优惠券抵扣
        BigDecimal totalCoupon = new BigDecimal("0.00");
        // 积分
        Integer giftGrowth = 0;
        // 成长值
        Integer giftIntegration = 0;
        for (OmsOrderItemEntity orderItem : orderItems) {
            totalPrice = totalPrice.add(orderItem.getRealAmount());
            totalPromotion = totalPromotion.add(orderItem.getPromotionAmount());
            totalIntegration = totalIntegration.add(orderItem.getIntegrationAmount());
            totalCoupon = totalCoupon.add(orderItem.getCouponAmount());
            giftGrowth +=  orderItem.getGiftGrowth();
            giftIntegration += orderItem.getGiftIntegration();
        }
        order.setTotalAmount(totalPrice);
        // 设置应付金额(订单总额+运费)
        order.setPayAmount(totalPrice.add(order.getFreightAmount()));
        order.setPromotionAmount(totalPromotion);
        order.setIntegrationAmount(totalIntegration);
        order.setCouponAmount(totalCoupon);
        order.setGrowth(giftGrowth);
        order.setIntegration(giftIntegration);
    }

    private OmsOrderEntity buildOrder(OrderSubmitVo vo, String orderSn) {
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();
        OmsOrderEntity orderEntity = new OmsOrderEntity();
        // 1. 设置订单号、创建时间、备注、支付方式
        orderEntity.setOrderSn(orderSn);
        orderEntity.setCreateTime(new Date());
        orderEntity.setPayType(vo.getPayType());
        orderEntity.setNote(vo.getNote());
        orderEntity.setMemberId(memberResVo.getId());
        orderEntity.setMemberUsername(memberResVo.getUsername());
        // 2. 获取收获地址信息
        R fare = wareFeignService.getFare(vo.getAddrId());
        if (fare.getCode() == 0){
            OrderFareVo fareResp = fare.getData("data", new TypeReference<OrderFareVo>() {
            });
            // 2.1 设置运费信息
            orderEntity.setFreightAmount(fareResp.getFare());
            // 2.2 设置收货地址信息
            //String receiverName;String receiverPhone;String receiverPostCode;
            // String receiverProvince;String receiverCity; String receiverRegion;
            // String receiverDetailAddress;
            orderEntity.setReceiverName(fareResp.getAddress().getName());
            orderEntity.setReceiverPhone(fareResp.getAddress().getPhone());
            orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareResp.getAddress().getProvince());
            orderEntity.setReceiverCity(fareResp.getAddress().getCity());
            orderEntity.setReceiverRegion(fareResp.getAddress().getRegion());
            orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        }
        // 3. 设置订单状态
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 自动确定
        orderEntity.setAutoConfirmDay(7);
        return orderEntity;
    }

    private List<OmsOrderItemEntity> buildOrderItems(String orderSn) {
        // 最后去确定每个购物项价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OmsOrderItemEntity> orderItems = currentUserCartItems.stream().map(cartItem -> {
                OmsOrderItemEntity orderItemEntity = buildCartItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return orderItems;
        }
        return null;
    }

    private OmsOrderItemEntity buildCartItem(OrderItemVo cartItem) {
        //Long skuId;String title;String ImageList<String> skuAttr;
        //BigDecimal price; Integer count; BigDecimal totalPrice;
        OmsOrderItemEntity orderItemEntity = new OmsOrderItemEntity();
        // 1. 商品的spu信息
        R r = productFeignService.getSpuInfoBySkuId(cartItem.getSkuId());
        if (r.getCode() == 0){
            SpuInfoVo data = r.getData("data", new TypeReference<SpuInfoVo>() {
            });
            orderItemEntity.setSpuId(data.getId());
            R brandInfo = productFeignService.getBrandInfo(data.getBrandId());
            if (brandInfo.getCode() == 0){
                BrandVo brand = brandInfo.getData("brand", new TypeReference<BrandVo>() {
                });
                orderItemEntity.setSpuBrand(brand.getName());
                orderItemEntity.setSpuName(data.getSpuName());
                orderItemEntity.setCategoryId(data.getCatalogId());
            }
        }
        // 2. 商品的sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToCommaDelimitedString(cartItem.getSkuAttr()));
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        // 3. 商品的优惠信息
        // 促销金额
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        // 优惠券优惠金额
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        // 积分优惠金额
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 优惠后的最终金额
        orderItemEntity.setRealAmount(orderItemEntity.getSkuPrice().
                multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()))
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount()));
        // 4. 积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        return orderItemEntity;
    }

}