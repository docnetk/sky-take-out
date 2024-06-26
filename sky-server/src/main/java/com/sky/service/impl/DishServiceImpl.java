package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.DishEnableFailedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);

        // 插入口味表
        dishMapper.insert(dish);

        // 插入风味表
        List<DishFlavor> flavors = dto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> pages = dishMapper.page(dishPageQueryDTO);
        return new PageResult(pages.getTotal(), pages.getResult());
    }

    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 判断是否存在菜品位于起售状态
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (StatusConstant.ENABLE.equals(dish.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 是否与套餐关联
        List<Long> setmealId = setmealDishMapper.getSetmealId(ids);
        if (setmealId != null && !setmealId.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        dishMapper.deleteBatch(ids);

        // 删除关联的口味
        dishFlavorMapper.deleteBatchWithDishId(ids);
    }

    @Override
    public DishVO getByIdWithFlavor(Long dishId) {
        Dish dish = dishMapper.getById(dishId);

        // 查询菜品对应的口味
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(dishId);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品信息
        dishMapper.update(dish);

        // 删除原来的口味
        List<Long> dishIds = new ArrayList<>();
        dishIds.add(dishDTO.getId());
        dishFlavorMapper.deleteBatchWithDishId(dishIds);

        // 添加新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dish.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {
        Dish dish = Dish.builder().categoryId(categoryId).status(StatusConstant.ENABLE).build();
        return dishMapper.selectByCategoryIdOrStatus(dish);
    }

    @Override
    public void modifyStatus(Integer status, Long id) {
        // 停售菜品。如果有起售的套餐包含该菜品，则不能停售
        if (StatusConstant.DISABLE.equals(status)) {
            List<Long> setmealIds = setmealDishMapper.getSetmealId(Collections.singletonList(id));
            if (setmealIds != null && !setmealIds.isEmpty()) {
                List<Setmeal> setmeals = setmealMapper.getByIds(setmealIds);
                for (Setmeal setmeal : setmeals) {
                    if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                        throw new DishEnableFailedException(MessageConstant.DISH_BE_CONTAINED_ON_SALE_SETMEAL);
                    }
                }
            }
        }
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.selectByCategoryIdOrStatus(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
