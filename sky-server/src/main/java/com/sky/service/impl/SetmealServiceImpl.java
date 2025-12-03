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
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class SetmealServiceImpl implements SetmealService {


    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;


    @Autowired
    private DishMapper dishMapper;


     /**
      * 新增套餐,同时需要保存套餐和菜品的关联关系
      * @param setMealDTO
      */
     @Transactional
    public void saveWithDish(SetmealDTO setMealDTO) {
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




     /**
      * 套餐分页查询
      * @param setmealPageQueryDTO
      * @return
      */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());


        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(),page.getResult());
    }



     /**
      * 删除套餐
      * @param ids
      */
     @Transactional
    public void deleteBatch(List<Long> ids) {

        //查询是否套餐启用
        ids.forEach(id ->{
            Setmeal setmeal = setmealMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });



        //delete from setmeal_dish where setmeal_id in (?,?,?)
        //删除套餐
        setmealMapper.deleteByIds(ids);


        //删除关联关系
        setmealDishMapper.deleteBysetmealIds(ids);
    }




     /**
      * 根据id查询套餐
      * @param id
      * @return
      */
    public SetmealVO getByIdWithDish(Long id) {

        //获取套餐信息
        Setmeal setmeal = setmealMapper.getById(id);

        //获取套餐菜品信息
        List<SetmealDish> setmealdishes = setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealdishes);

        return setmealVO;
    }



     /**
      * 修改套餐
      * @param setmealDTO
      */
     @Transactional
    public void update(SetmealDTO setmealDTO) {
        //修改套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

        //获取套餐id
        Long setmealid = setmeal.getId();

        //修改菜品和套餐关联信息
        //此处改为删除原有关联信息,再重新增添新的关联信息
        List<Long> list = new ArrayList<>();
        list.add(setmealid);
        setmealDishMapper.deleteBysetmealIds(list);


        List<SetmealDish> setmealdishes = setmealDTO.getSetmealDishes();

        //设置菜品对应的套餐id
        setmealdishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealid);
        });

        setmealDishMapper.insertBatch(setmealdishes);

    }

     /**
      * 套餐起售停售
      * @param status
      * @param id
      */
    public void StartOrStop(Integer status,Long id){
        //检查套餐状态,看是否有停售的菜品，有则不起售
        if(StatusConstant.ENABLE == status){
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if(dishes!=null && dishes.size()>0){
                dishes.forEach(dish -> {
                    //获取菜品的起售状态
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }


        //修改套餐状态
        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(id);
        setmealMapper.update(setmeal);
    }


    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
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
