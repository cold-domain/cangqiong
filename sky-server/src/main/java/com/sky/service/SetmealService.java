package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;

public interface SetmealService {


     /**
      * 新增套餐
      * @param setMealDTO
      */
    void saveWithDish(SetmealDTO setMealDTO);



     /**
      * 分页查询套餐
      * @param setmealPageQueryDTO
      * @return
      */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
}
