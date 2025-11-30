package com.sky.service.impl;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SetmealServiceImpl implements SetmealService {


    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


     /**
      * 新增套餐,同时需要保存套餐和菜品的关联关系
      * @param setMealDTO
      */
    public void save(SetmealDTO setMealDTO) {
        // 新增套餐
        Setmeal setMeal = new Setmeal();
        BeanUtils.copyProperties(setMealDTO,setMeal);

        setmealMapper.insert(setMeal);


        //获取套餐id
        Long setMealId = setMeal.getId();

        //设置套餐和菜品的关联id
        List<SetmealDish> setmealdishes = setMealDTO.getSetmealDishes();
        setmealdishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setMealId);
        });

        //新增套餐和菜品的关系
        setmealDishMapper.insertBatch(setmealdishes);
    }
}
