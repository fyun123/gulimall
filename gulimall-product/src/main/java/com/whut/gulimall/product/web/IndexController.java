package com.whut.gulimall.product.web;

import com.whut.gulimall.product.entity.CategoryEntity;
import com.whut.gulimall.product.feign.CartFeignService;
import com.whut.gulimall.product.service.CategoryService;
import com.whut.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    CartFeignService cartFeignService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model) throws ExecutionException, InterruptedException {
        // 查出所有的一级分类
         List<CategoryEntity> categoryEntities = categoryService.getLevel1Category();
         model.addAttribute("categories", categoryEntities);
        Integer countNum = cartFeignService.getCountNum();
        model.addAttribute("countNum",countNum);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String,List<Catalog2Vo>> getCatalogJson(){
        Map<String,List<Catalog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        // 获取一把锁，只要锁的名字相同，就是同一把锁
        RLock lock = redisson.getLock("my-lock");
        // 加锁
//        lock.lock(); //阻塞式等待，默认加锁30s
        lock.lock(10, TimeUnit.SECONDS);
        // 1）锁的自动续期，如果业务时间超长，运行期间自动给锁续上30s.不必当心业务时间长，锁子动过期被删掉
        // 2）锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s后自动删除
        try{
            System.out.println("加锁成功，执行业务"+Thread.currentThread().getId());
            Thread.sleep(10000);
        }catch (Exception e){

        }finally {
            // 解锁
            System.out.println("释放锁"+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    /**
     * 读写锁
     */
    @ResponseBody
    @GetMapping("/write")
    public String write(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            rLock.lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(10000);
            redisTemplate.opsForValue().set("write",s);

        }catch (Exception e){
        }finally {
            rLock.unlock();
        }
        return s;
    }

    @ResponseBody
    @GetMapping("/read")
    public String read(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.readLock();
        try {
            rLock.lock();
            s = redisTemplate.opsForValue().get("write");
        }catch (Exception e){
        }finally {
            rLock.unlock();
        }
        return s;
    }

    /**
     * 信号量:可以用作限流
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
//        park.acquire(); // 获取一个信号，获取一个值，占一个车位
        park.tryAcquire(); //返回true,false
        return "ok";
    }

    @GetMapping("/gogo")
    @ResponseBody
    public String go(){
        RSemaphore park = redisson.getSemaphore("park");
        park.release(); //释放一个车位
        return "ok";
    }
    /**
     * 闭锁
     */
    @GetMapping("/lockdoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();
        return "闭锁了";
    }

    @GetMapping("/go/{id}")
    @ResponseBody
    public String go(@PathVariable("id") Long id){
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown(); // 计数减一
        return id+"锁释放了";
    }
}
