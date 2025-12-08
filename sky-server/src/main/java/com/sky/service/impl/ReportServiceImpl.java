package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;


    /**
     * 统计指定时间区间内的营业额数据
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放指定时间区间的所有日期
        List<LocalDate> dateList = new ArrayList<>();

        //添加日期
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //查询营业额数据，遍历日期集合，查询数据库
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期的营业额，营业额：状态为“已完成”的订单金额合计
            //此处date与order_time类型不同，需要设置好date的起始时间和结束时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);


            //select sum(amount) from orders
            // where order_time > beginTime and order_time < endTime and status = 5
            //封装三个参数到map中
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);

            //如果查询结果为空，将营业额设置为0.0
            turnover = turnover == null ? 0.0 : turnover;

            turnoverList.add(turnover);
        }


        //封装结果，返回转换为字符串的日期列表和转换为字符串的营业额列表
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }



    /**
     * 统计指定时间区间内的用户数据
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        //处理日期集合
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //处理新增用户和用户总量集合
        //每天新增用户数
        //select count(id) from user where create_time < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();
        //每天用户总量
        //select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();


        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end",endTime);

            //总用户数
            Integer totalUser = userMapper.countByMap(map);

            //新增用户数
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        //封装结果数据
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }


    /**
     * 统计指定时间区间内的订单数据
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //处理日期集合
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //处理每天订单数和有效订单数
        List<Integer> orderCoutnList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //每天订单数
            //select count(id) from orders where order_time > ? and order_time < ?

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //每天有效订单数
            //select count(id) from orders where order_time > ? and order_time < ? and status = 5
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCoutnList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCoutnList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCoutnList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    /**
     * 根据条件查询订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);

        return orderMapper.countByMap(map);
    }



    /**
     * 统计指定时间区间内的销量top10商品数据
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        /*select od.name,sum(od.number) number from order_detail od,orders o
        where od.order_id = o.id and o.status = 5 and o.order_time > ? and o.order_time < ?
        group by od.name
        order by number desc
        limit 0,10*/

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);

        List<GoodsSalesDTO> salesDTOList = orderMapper.getSalesTop10(beginTime, endTime);


        List<String> names = salesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names,",");

        List<Integer> numbers = salesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers,",");

        return SalesTop10ReportVO.builder()
                .numberList(numberList)
                .nameList(nameList)
                .build();
    }
}
