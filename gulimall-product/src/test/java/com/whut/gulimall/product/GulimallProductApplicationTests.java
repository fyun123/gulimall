package com.whut.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.whut.gulimall.product.entity.BrandEntity;
import com.whut.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setDescript("手机");
//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//        brandEntity.setBrandId(1L);
//        brandEntity.setFirstLetter("H");
//        brandService.updateById(brandEntity);
        List<BrandEntity> entities = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        entities.forEach((entity)->{
            System.out.println(entity);
                });
    }

}
