package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {


    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //并不是简单的insert即可，
        //需要判断商品是否存在在购物车中，根据商品的情况执行不同的操作
        //判断商品是否存在在购物车中，使用动态sql语句
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //如果存在，只需要数量增加1，执行update语句
        if(list!=null && list.size()>0){
            ShoppingCart cart = list.get(0);    //有且仅有一条数据
            cart.setNumber(cart.getNumber()+1);
            //update shopping_cart set number = ? where id = ?
            shoppingCartMapper.updateNumberById(cart);
        }else {
            //不存在，插入购物车数据
            //判断是菜品还是套餐
            Long dishId = shoppingCart.getDishId();
            if(dishId!=null){
                //本次添加的是菜品
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());

            }else {
                //本次添加的是套餐
                Long setmealId = shoppingCart.getSetmealId();

                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.Insert(shoppingCart);

        }


    }



     /**
      * 查询购物车
      * @return
      */
    public List<ShoppingCart> showShoppingCart(){
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        //构造查询参数
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        return list;
    }



     /**
      * 清空购物车
      */
    public void cleanShoppingCart(){
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        shoppingCartMapper.deleteByUserId(userId);
    }
}
