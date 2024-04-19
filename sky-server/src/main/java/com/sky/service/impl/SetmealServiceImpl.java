package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    @CacheEvict(value = "Setmeal", key = "#setmealDTO.categoryId")
    public void save(SetmealDTO setmealDTO) {
        // 将套餐信息保存至套餐表
        Setmeal setmeal = Setmeal.builder().categoryId(setmealDTO.getCategoryId())
                .name(setmealDTO.getName())
                .price(setmealDTO.getPrice())
                .status(StatusConstant.DISABLE)
                .description(setmealDTO.getDescription())
                .image(setmealDTO.getImage())
                .build();
        setmealMapper.insert(setmeal);

        // 将套餐包含的菜品保存
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes == null || setmealDishes.isEmpty()) {
            return;
        }
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        System.out.println(setmealDishes);
        setmealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealPageQueryDTO, setmeal);
        Page<SetmealVO> page = (Page<SetmealVO>) setmealMapper.pageQuery(setmeal);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    @CacheEvict(value = "Setmeal", allEntries = true)
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Setmeal> setmeals = setmealMapper.getByIds(ids);
        for (Setmeal setmeal : setmeals) {
            if (setmeal.getStatus().equals(StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        // 批量删除套餐
        setmealMapper.deleteByIds(ids);
        // 批量套餐菜品表中的与套餐有关的菜品信息
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    @Override
    public SetmealVO getById(Long id) {
        // 查询套餐
        List<Setmeal> setmeals = setmealMapper.getByIds(Collections.singletonList(id));
        Setmeal setmeal = setmeals.get(0);

        // 查到与套餐关联的菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getById(setmeal.getId());

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    @CacheEvict(value = "Setmeal", allEntries = true)
    public void update(SetmealDTO setmealDTO) {
        // 先修改套餐的基本信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        // 修改套餐包含的菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        //删除原来的菜品信息
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmeal.getId()));

        // 添加新的菜品信息
        setmealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    @CacheEvict(value = "Setmeal", allEntries = true)
    public void modifyStatus(Integer status, Long id) {
        if (StatusConstant.ENABLE.equals(status)) {
            // 起售套餐, 需要先判断是否套餐包含的菜品是否已停售
            List<SetmealDish> dishes = setmealDishMapper.getById(id);
            for (SetmealDish setmealDish : dishes) {
                Dish dish = dishMapper.getById(setmealDish.getId());
                if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        setmealMapper.update(setmeal);
    }

    @Cacheable(value = "Setmeal", key = "#setmeal.categoryId")
    public List<Setmeal> list(Setmeal setmeal) {
        return setmealMapper.list(setmeal);
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
