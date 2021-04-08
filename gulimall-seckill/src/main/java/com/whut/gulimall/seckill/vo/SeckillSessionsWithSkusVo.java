package com.whut.gulimall.seckill.vo;


import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class SeckillSessionsWithSkusVo {
    /**
     * id
     */
    private Long id;
    /**
     * 场次名称
     */
    private String name;
    /**
     * 每日开始时间
     */
    private Date startTime;
    /**
     * 每日结束时间
     */
    private Date endTime;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 秒杀活动关联的Sku信息
     */
    private List<SeckillSkuRelationVo> relationSkus;
}
