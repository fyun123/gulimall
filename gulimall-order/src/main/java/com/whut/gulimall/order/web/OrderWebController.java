package com.whut.gulimall.order.web;


import com.whut.common.exception.NoStockException;
import com.whut.gulimall.order.service.OmsOrderService;
import com.whut.gulimall.order.vo.OrderConfirmVo;
import com.whut.gulimall.order.vo.OrderSubmitVo;
import com.whut.gulimall.order.vo.SubmitOrderResponseVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OmsOrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(@RequestParam(value = "orderSn",required = false) String orderSn,OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes) {

        if (StringUtils.isEmpty(orderSn)){
            try {
                SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
                if (responseVo.getCode() == 0) {
                    model.addAttribute("submitOrderResp", responseVo);
                    return "pay";
                } else {
                    String msg = "下单失败：";
                    switch (responseVo.getCode()) {
                        case 1:
                            msg += "订单信息过期，请刷新后再次提交";
                            break;
                        case 2:
                            msg += "订单商品价格发生变化，请确认后再次提交";
                            break;
//                    case 3:
//                        msg += "商品库存不足，请重新下单";
//                        redirectAttributes.addFlashAttribute("failSkuIds", responseVo.getFailSkuIds());
//                        break;
                        default:
                            break;
                    }
                    redirectAttributes.addFlashAttribute("msg", msg);
                    return "redirect:http://order.gulimall.com/toTrade";
                }
            }catch (NoStockException e){
                redirectAttributes.addFlashAttribute("msg", "商品库存不足，请重新下单");
                redirectAttributes.addFlashAttribute("failSkuIds", e.getSkuIds());
                return "redirect:http://order.gulimall.com/toTrade";
            }
        }else {
            SubmitOrderResponseVo responseVo = orderService.submitSeckillOrder(orderSn,vo);
            model.addAttribute("submitOrderResp", responseVo);
            return "pay";
        }
    }

    //toSeckillConfirm
    @GetMapping("/toSeckillConfirm")
    public String toSeckillConfirm(@RequestParam("orderSn") String orderSn, Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.toSeckillConfirm(orderSn);
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }
}
