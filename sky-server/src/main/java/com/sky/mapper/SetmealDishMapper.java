package com.sky.mapper;


import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {


    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);




    /**
     * 批量插入套餐菜品关系
     * @param setmealdishes
     */
    void insertBatch(List<SetmealDish> setmealdishes);



     /**
      * 根据套餐id删除套餐菜品关系
      * @param ids
      */
    void deleteBysetmealIds(List<Long> ids);
}
