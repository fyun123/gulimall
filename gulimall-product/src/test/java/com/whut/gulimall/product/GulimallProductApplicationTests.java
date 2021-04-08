package com.whut.gulimall.product;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.whut.gulimall.product.dao.AttrGroupDao;
import com.whut.gulimall.product.dao.SkuSaleAttrValueDao;
import com.whut.gulimall.product.entity.BrandEntity;
import com.whut.gulimall.product.service.BrandService;
import com.whut.gulimall.product.service.CategoryService;
import com.whut.gulimall.product.vo.SkuItemSaleAttrVo;
import com.whut.gulimall.product.vo.SkuItemVo;
import com.whut.gulimall.product.vo.SpuItemBaseAttrVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    private BrandService brandService;

    @Resource
    private OSS ossClient;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
     AttrGroupDao attrGroupDao;
    @Resource
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void test(){
        List<SpuItemBaseAttrVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(2L, 225L);
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(2L);
        System.out.println(saleAttrsBySpuId);
    }

    @Test
    public void testRedisson(){
        System.out.println(redissonClient);
    }

    @Test
    public void testRedis(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello","你好");
        String hello = ops.get("hello");
        System.out.println(hello);
    }

    @Test
    public void findParent(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("完整路径", Arrays.asList(catelogPath));
    }

    @Test
    public void testUpload() throws FileNotFoundException {
//        // Endpoint以杭州为例，其它Region请按实际情况填写。
//        String endpoint = "oss-cn-guangzhou.aliyuncs.com";
//        // 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
//        String accessKeyId = "LTAI4G2bGvDnAbdjUYkVBpgY";
//        String accessKeySecret = "MES7XMsiPjedYemXSBCRYWrfdwAVV2";
//
//        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        // 上传文件流。
        InputStream inputStream = new FileInputStream("F:\\图片\\logo.png");
        ossClient.putObject("gulimall-fyun", "logo3.png", inputStream);
//
//        // 关闭OSSClient。
//        ossClient.shutdown();
        System.out.println("上传成功");
    }

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
