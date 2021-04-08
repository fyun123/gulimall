package com.whut.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.whut.gulimall.product.config.MyCacheConfig;
import com.whut.gulimall.product.service.CategoryBrandRelationService;
import com.whut.gulimall.product.vo.Catalog2Vo;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.product.dao.CategoryDao;
import com.whut.gulimall.product.entity.CategoryEntity;
import com.whut.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1. 查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2. 组装成父子树形结构
        // 2.1 找到所有的一级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter((categoryEntity) -> categoryEntity.getParentCid() == 0).map((menu) -> {
            menu.setChildren(getChildren(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 获取节点的完整路径
     * 父/子/孙
     *
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, path);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Category'"),
//            @CacheEvict(value = "category", key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }


    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Category() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    @Cacheable(value = {"category"}, key = "#root.method.name",sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        // 查出所有分类
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 查出所有一级分类
        List<CategoryEntity> level1Category = getParent_cid(selectList, 0L);
        // 封装数据
        Map<String, List<Catalog2Vo>> catalog2 = level1Category.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), l1 -> {
            // 获取当前一级分类下的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, l1.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(l1.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 封装当前二级分类的三级分类
                    List<CategoryEntity> category3 = getParent_cid(selectList, l2.getCatId());
                    if (category3 != null) {
                        List<Catalog2Vo.Catalog3Vo> catalog3Vos = category3.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        return catalog2;
    }

    /**
     * 1、空结果缓存：解决缓存穿透
     * 2、设置过期时间（加随机值）：解决缓存雪崩
     * 3、加锁：解决缓存击穿
     *
     * @return
     */
//    public Map<String, List<Catalog2Vo>> getCatalogJson1() {
//        // 给缓存中放入JSON字符串，拿出JSON字符串，还要逆转为能用的对象类型。【序列化与反序列化】
//        // 加入缓存逻辑,缓存中存的数据是Json字符串
//        // JSON跨语言，跨平台兼容
//        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
//        if (StringUtils.isEmpty(catalogJson)) {
//            System.out.println("缓存不命中，查询数据库....");
//            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbLocalLock();
//            return catalogJsonFromDb;
//        }
//        System.out.println("缓存命中，直接返回");
//        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
//        });
//        return result;
//    }

    /**
     * redisson分布式锁
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbRedissonLock() {
        // 1. 占分布式锁，锁的粒度，越细越快
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catalog2Vo>> dataFromDb;
        try{
            dataFromDb = getDataFromDb();
        }finally {
            lock.unlock();
        }
        return dataFromDb;

    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbRedisLock() {
        // 加锁，同时设置过期时间
        String token = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.SECONDS);
        if (lock) {
            // 加锁成功，执行业务
            // 设置过期时间，必须和加锁是同步的，原子的
//            stringRedisTemplate.expire("lock",30, TimeUnit.SECONDS);
            Map<String, List<Catalog2Vo>> dataFromDb;
            try{
                dataFromDb = getDataFromDb();
            }finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),Arrays.asList("lock"),token);
            }

            //获取值比对+对比成功删除锁=原子操作，lua脚本解锁
//            if (token.equals(stringRedisTemplate.opsForValue().get("lock"))){
//                // 删除自己的锁
//                stringRedisTemplate.delete("lock");
//            }
            return dataFromDb;
        } else {
            // 重试
            try {
                Thread.sleep(100);
            } catch (Exception e) {

            }
            return getCatalogJsonFromDbRedisLock(); // 自旋的方式
        }
    }

    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        // 先看缓存中有没有
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            System.out.println("有缓存了，不查询数据库了....");
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
            return result;
        }
        // 查出所有分类
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 查出所有一级分类
        List<CategoryEntity> level1Category = getParent_cid(selectList, 0L);
        // 封装数据
        Map<String, List<Catalog2Vo>> catalog2 = level1Category.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), l1 -> {
            // 获取当前一级分类下的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, l1.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(l1.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 封装当前二级分类的三级分类
                    List<CategoryEntity> category3 = getParent_cid(selectList, l2.getCatId());
                    if (category3 != null) {
                        List<Catalog2Vo.Catalog3Vo> catalog3Vos = category3.stream().map(l3 -> {
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        String s = JSON.toJSONString(catalog2);
        stringRedisTemplate.opsForValue().set("catalogJson", s);
        return catalog2;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbLocalLock() {
        synchronized (this) {
            return getDataFromDb();
        }

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
        return collect;
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l1.getCatId()));
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        //收集当前节点id
        path.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), path);
        }
        return path;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return Objects.equals(categoryEntity.getParentCid(), root.getCatId());
        }).map(categoryEntity -> {
            //找到子菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //菜单排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}