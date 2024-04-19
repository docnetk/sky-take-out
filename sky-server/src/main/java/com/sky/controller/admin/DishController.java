package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("admin/dish")
@Slf4j
public class DishController {
    private final String PREFIX = "dish_";

    @Autowired
    private DishService dishService;

    @Autowired
    private StringRedisTemplate redisTemplate;
    // TODO 为什么自己配置的RedisTemplate不起作用

    @PostMapping
    public Result saveWithFlavor(@RequestBody DishDTO dishDTO) {
        dishService.saveWithFlavor(dishDTO);

        Long categoryId = dishDTO.getCategoryId();
        clearRedis(PREFIX + categoryId);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    public Result deleteBatch(@RequestParam List<Long> ids) {
        dishService.deleteBatch(ids);
        clearRedis(PREFIX + "*");
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        dishService.updateWithFlavor(dishDTO);
        // 因为此菜品可能更改分类，从而影响多个分类
//        System.out.println("修改王老吉");
        clearRedis(PREFIX + "*");
        return Result.success();
    }

    @GetMapping("/list")
    public Result<Object> getByCategoryId(@RequestParam(value = "categoryId") Long id) {
        List<Dish> dishes = dishService.getByCategoryId(id);
        return Result.success(dishes);
    }

    @PostMapping("status/{status}")
    public Result modifyStatus(@PathVariable Integer status, Long id) {
        dishService.modifyStatus(status, id);
        /*
        此处也可查询菜品对应分类后，清除此分类对应缓存
        但停售菜品的操作不频繁
         */
        clearRedis(PREFIX + "*");
        return Result.success();
    }

    private void clearRedis(String pattern) {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        Set keys = redisTemplate.keys(pattern);
//        if (keys == null || keys.isEmpty()) {
//            return;
//        }

        redisTemplate.delete(keys);
    }
}
