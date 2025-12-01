package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {


    @Autowired
    private SetmealService setMealService;

    /**
     * 新增套餐
     * @param setMealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setMealDTO){
        log.info("新增套餐:{}",setMealDTO);
        setMealService.saveWithDish(setMealDTO);
        return Result.success();
    }




    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询套餐")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐:{}",setmealPageQueryDTO);
        PageResult page = setMealService.pageQuery(setmealPageQueryDTO);
        return Result.success(page);
    }




     /**
      * 批量删除套餐
      * @param ids
      * @return
      */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除套餐:{}",ids);
        setMealService.deleteBatch(ids);
        return Result.success();
    }



    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐:{}",id);
        SetmealVO setmealvo = setMealService.getByIdWithDish(id);
        return Result.success(setmealvo);
    }


    @PutMapping
    @ApiOperation("更新套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("更新套餐:{}",setmealDTO);
        setMealService.update(setmealDTO);
        return Result.success();
    }
}
