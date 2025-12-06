package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1.处理各种业务异常（地址簿为空，购物车数据为空）
        //校验地址簿信息
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //校验购物车信息
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if(list == null || list.size() == 0){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        //2.向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orders.setAddress(addressBook.getDetail());

        //此处需要返回主键值，才能为后续的订单详情表插入数据提供订单id
        orderMapper.insert(orders);

        //3.向订单详情表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : list){
            OrderDetail orderDetail = new OrderDetail();    //订单明细，每个购物车商品项对应一个订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());         //设置当前订单明细所属订单id
            orderDetailList.add(orderDetail);
        }
        //批量插入
        orderDetailMapper.insertBatch(orderDetailList);


        //4.清空用户购物车数据
        shoppingCartMapper.deleteByUserId(userId);


        //5.封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }



    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        /*//调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
        /*log.info("跳过微信支付，支付成功");

        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();*/
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }




    /**
     * 查询历史订单
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        //查询订单，再查询明细，展示菜品及价格等信息

        //订单表包含订单详细表，故而此时订单详细表为订单表的一个属性
        //通过订单id查询对应的订单详情

        //设置分页
        PageHelper.startPage(pageNum,pageSize);

        //设置查询DTO
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        //分页查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);


        //查询订单明细，封装成VO集合返回
        List<OrderVO> list = new ArrayList<>();
        if(page!=null && page.size()>0){
            for (Orders orders : page) {
                //取订单id，此处需要在mapper中设置返回主键
                Long ordersId = orders.getId();

                //查订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(ordersId);

                //封装VO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                //加入VO集合
                list.add(orderVO);
            }
        }

        return new PageResult(page.getTotal(),list);
    }



    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    public OrderVO details(Long orderId){
        //查两张表，一张订单表，一张订单详细表，之后将其封装到VO对象中进行返回即可

        //查订单表，1项
        Orders orders = orderMapper.getById(orderId);

        //查订单详细表，n项
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        //封装VO对象
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }


    /**
     * 用户取消订单
     * @param orderId
     */
    public void userCancelById(Long orderId) {

        //1.获取订单数据及订单状态
        Orders orders = orderMapper.getById(orderId);

        //判断订单是否存在
        if(orders == null)
        {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //获取订单详情
        Integer status = orders.getStatus();

        //2.判断订单的情况，根据不同状态进行处理
        //商家已接单状态和派送中状态，用户取消订单需电话沟通商家
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orderTemp = new Orders();
        orderTemp.setId(orders.getId());

        //待接单状态下取消订单，需要给用户退款
        if(orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //退款代码
            //调用微信支付退款接口
            //  weChatPayUtil.refund(
            //          ordersDB.getNumber(), //商户订单号
            //          ordersDB.getNumber(), //商户退款单号
            //          new BigDecimal(0.01),//退款金额，单位 元
            //          new BigDecimal(0.01));//原订单金额

            //修改订单支付状态为退款
            orders.setPayStatus(Orders.REFUND);
        }



        //待支付和待接单状态下，用户可直接取消订单
        //3.取消订单后需要将订单状态修改为“已取消”
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    /**
     * 再来一单
     * @param orderId
     */
    public void repetition(Long orderId) {
        //1.查询当前用户id
        Long userId = BaseContext.getCurrentId();

        //2.查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        //3.订单详情对象转为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(orderDetail -> {
            //构建新的购物车对象
            ShoppingCart shoppingCart = new ShoppingCart();

            //复制属性(除去id项)，设置好字段
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());


        //4.购物车对象批量添加
        shoppingCartMapper.insertBatch(shoppingCartList);
    }



     /**
      * 管理员端根据条件查询订单
      * @param ordersPageQueryDTO
      * @return
      */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }


    /**
     * 将Orders对象列表转换为OrderVO对象列表
     * @param page 包含Orders对象的分页结果
     * @return 包含OrderVO对象的列表
     */
    public List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        for(Orders orders : ordersList){
            //新建VO对象
            OrderVO orderVO = new OrderVO();

            //复制属性到VO对象
            BeanUtils.copyProperties(orders,orderVO);

            //获取菜品信息字符串
            String orderDishes = getOrderDishesStr(orders);

            //设置VO对象的orderDishes属性
            orderVO.setOrderDishes(orderDishes);

            //将VO对象添加到列表
            orderVOList.add(orderVO);
        }

        return orderVOList;
    }


    /**
     * 获取订单菜品信息字符串
     * @param orders 订单对象
     * @return 订单菜品信息字符串（格式：宫保鸡丁*3；）
     */
    public String getOrderDishesStr(Orders orders){
        //查询订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishesList = orderDetailList.stream()
                .map(orderDetail ->{
            String orderDish = orderDetail.getName() + "*" + orderDetail.getNumber();
            return orderDish;
        }).collect(Collectors.toList());


        return String.join(",",orderDishesList);
    }



    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics(){
        //查询不同状态的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //封装VO对象
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }


    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {

        //创建orders对象
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        //修改订单状态
        orderMapper.update(orders);
    }



    /**
     * 商家拒绝订单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //查询订单状态
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());

        //判断订单是否存在且订单状态是否为“待接单”，是才可以取消
        if(orders == null || !orders.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //若订单已完成支付，需要退款
        Integer payStatus = orders.getPayStatus();
        if(payStatus == Orders.PAID){
            //调用退款代码
            //用户已支付，需要退款
            /*String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
                    */

            log.info("用户申请退款");
        }

        //更新订单状态为“已取消”及相关字段
        Orders ordersDB = new Orders();
        ordersDB.setId(orders.getId());
        ordersDB.setStatus(Orders.CANCELLED);
        ordersDB.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        ordersDB.setCancelTime(LocalDateTime.now());

        orderMapper.update(ordersDB);
    }



    public void cancel(OrdersCancelDTO ordersCancelDTO) {

        //查询订单是否存在
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //创建订单修改对象
        Orders ordersDB = new Orders();
        ordersDB.setId(orders.getId());
        ordersDB.setStatus(Orders.CANCELLED);
        ordersDB.setCancelReason(ordersCancelDTO.getCancelReason());
        ordersDB.setCancelTime(LocalDateTime.now());

        orderMapper.update(ordersDB);
    }


    public void delivery(Long orderId){

        //查询订单存在以及其状态是否为“待派送”
        Orders orders = orderMapper.getById(orderId);

        if(orders == null || !orders.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        //修改订单状态
        Orders orderTemp = new Orders();
        orderTemp.setId(orderId);
        orderTemp.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orderTemp);
    }
}
