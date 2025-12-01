package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

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



     /**
      * 删除套餐
      * @param ids
      */
    void deleteBatch(List<Long> ids);



     /**
      * 根据id查询套餐
      * @param id
      * @return
      */
    SetmealVO getByIdWithDish(Long id);



     /**
      * 更新套餐
      * @param setmealDTO
      */
    void update(SetmealDTO setmealDTO);



     /**
      * 启用或停用套餐
      * @param status
      * @param id
      */
    void StartOrStop(Integer status, Long id);
}
