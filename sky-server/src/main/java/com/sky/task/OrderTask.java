package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * *")        //每分钟触发一次
    //@Scheduled(cron = "3/5 * * * * *")        //测试时间
    public void processTimeoutOrder(){
        log.info("处理超时订单:{}", LocalDateTime.now());

        //获取下单时间
        LocalDateTime orderTime = LocalDateTime.now().plusMinutes(-15);

        //查询是否有超时订单
        //select * from orders where status = ? and order_time < (当前时间-15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, orderTime);

        //更新订单状态
        if(ordersList!=null && ordersList.size()>0){
            for(Orders order: ordersList){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());

                orderMapper.update(order);
            }
        }
    }



    /**
     * 处理配送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")   //每天凌晨1点触发一次
    //@Scheduled(cron = "0/5 * * * * *")   //测试时间
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单状态:{}", LocalDateTime.now());

        //你可以理解成商家12点打烊了，1点就把订单都清理了。此处处理的是前一天12点前下单的订单
        //若要设置为2点，则改为-2，以此类推
        LocalDateTime time = LocalDateTime.now().plusHours(-1);


        //查询派送中的订单
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        //更新订单状态
        if(ordersList!=null && ordersList.size()>0){
            for(Orders order: ordersList){
                order.setStatus(Orders.COMPLETED);

                orderMapper.update(order);
            }
        }
    }
}
