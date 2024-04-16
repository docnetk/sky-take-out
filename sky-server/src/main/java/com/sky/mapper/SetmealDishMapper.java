package com.sky.mapper;

import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品Id的得到对应的套餐Id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealId(List<Long> dishIds);

    // 不需要加AutoFill注解
    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBySetmealIds(List<Long> ids);

    @Select("select * from setmeal_dish where setmeal_id=#{id}")
    List<SetmealDish> getById(Long id);
}
