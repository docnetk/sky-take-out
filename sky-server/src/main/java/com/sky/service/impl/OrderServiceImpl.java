package com.sky.service.impl;

import com.alibaba.fastjson2.JSONObject;
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
import com.sky.utils.WebSocketServer;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 先校验地址和购物车是否为空
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 生成订单
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);

        order.setOrderTime(LocalDateTime.now());
        // 订单号有时间戳和用户id组成
        order.setNumber(String.valueOf(System.currentTimeMillis()) + userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setUserId(userId);
        order.setPayStatus(Orders.UN_PAID);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        // 可以根据user表的name字段获得，就是微信用户的昵称
//        order.setUserName();
        orderMapper.insert(order);

        // 生成订单明细
        List<OrderDetail> orderDetails = new ArrayList<>();
        // 该用户购物车的所有的商品
        for (ShoppingCart item : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail);
            orderDetail.setId(null);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        // 清空购物车
        System.out.println("提交订单，清空购物车");
        shoppingCartMapper.delete(shoppingCart);

        // 返回结果
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal("0.01"), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        // 默认认为“点击确定”即支付成功，更新订单状态
        Orders orders = new Orders();
        orders.setNumber(ordersPaymentDTO.getOrderNumber());
        orders.setPayStatus(Orders.PAID);
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setCheckoutTime(LocalDateTime.now());
        orderMapper.updateStatus(orders);

        // 通知商家接单
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
        return vo;
    }

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

    @Override
    public PageResult getHistoricalOrders(int pageNum, int pageSize, Integer status) {
        // 构造查询条件
        OrdersPageQueryDTO dto = new OrdersPageQueryDTO();
        dto.setStatus(status);
        dto.setUserId(BaseContext.getCurrentId());

        // 查询订单
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = orderMapper.queryOrders(dto);

        Page<Orders> pageOrders = orderMapper.queryOrders(dto);
        // 将每个订单和其详细信息
        List<OrderVO> orderVOS = new ArrayList<>();
        if (pageOrders != null && !pageOrders.isEmpty()) {
            for (Orders order : pageOrders) {
                List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(order.getId());
//                String orderDishes = JSONObject.toJSONString(order);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetails);

//                System.out.println(orderVO.getOrderDishes());
                orderVOS.add(orderVO);
//                System.out.println(orderVO);
                /*
                TODO orderVO中的orderDishes为null，但去掉BeanUtils.copyProperties(order, orderVO)却跑不通
                 */
            }
        }
        return new PageResult(pageOrders.getTotal(), orderVOS);
    }

    @Override
    public OrderVO orderDetail(Long id) {
        Orders order = orderMapper.selectById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);

        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    @Override
    public void cancelOrder(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            return;
        }
        Integer orderStatus = orders.getStatus();

        if (orderStatus > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (orderStatus.equals(Orders.TO_BE_CONFIRMED)) {
            // 订单状态为待接单，需要进行退款
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(id);
        if (orderDetails == null || orderDetails.isEmpty()) {
            return;
        }
        Long userId = BaseContext.getCurrentId();

        for (OrderDetail orderDetail : orderDetails) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public PageResult searchOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOS = new ArrayList<>();
        for (Orders order : orders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            String orderDishesString = getOrderDishesString(order);
            orderVO.setOrderDishes(orderDishesString);
            orderVOS.add(orderVO);
        }
        return new PageResult(orders.getTotal(), orderVOS);
    }

    @Override
    public OrderStatisticsVO statisticsOrder() {
        OrderStatisticsVO statisticsVO = new OrderStatisticsVO();
        List<Orders> confirmedCount = orderMapper.selectByStatus(Orders.CONFIRMED);
        if (confirmedCount != null) {
            statisticsVO.setConfirmed(confirmedCount.size());
        }

        List<Orders> list = orderMapper.selectByStatus(Orders.DELIVERY_IN_PROGRESS);
        if (list != null) {
            statisticsVO.setDeliveryInProgress(list.size());
        }

        list = orderMapper.selectByStatus(Orders.TO_BE_CONFIRMED);
        if (list != null) {
            statisticsVO.setDeliveryInProgress(list.size());
        }
        return statisticsVO;
    }

    @Override
    public void receiveOrder(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (orders != null) {
            if (Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
                orders.setStatus(Orders.CONFIRMED);
                orderMapper.update(orders);
            }
        }
    }

    @Override
    public void rejectOrder(OrdersRejectionDTO rejectionDTO) {
        Orders order = orderMapper.selectById(rejectionDTO.getId());
        if (order != null) {
            if (Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
                order.setRejectionReason(rejectionDTO.getRejectionReason());
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
            } else {
                throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
            if (Orders.PAID.equals(order.getPayStatus())) {
                // 需要退款，未来保险做一个判断
                order.setPayStatus(Orders.REFUND);
            }
            orderMapper.update(order);
        }
    }

    @Override
    public void cancelOrderByAdmin(OrdersCancelDTO ordersCancelDTO) {
        Orders order = orderMapper.selectById(ordersCancelDTO.getId());
        if (order != null) {
            if (Orders.PAID.equals(order.getStatus())) {
                // 为用户退款
                System.out.println(LocalDateTime.now() + "  退款成功!");
            }
            order.setStatus(Orders.CANCELLED);
            order.setCancelReason(ordersCancelDTO.getCancelReason());
            order.setCancelTime(LocalDateTime.now());

            orderMapper.update(order);
        }
    }

    @Override
    public void delivery(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order != null) {
            if (Orders.CONFIRMED.equals(order.getStatus())) {
                order.setStatus(Orders.DELIVERY_IN_PROGRESS);

                orderMapper.update(order);
            } else {
                throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
        }
    }

    @Override
    public void completeOrder(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order != null) {
            if (Orders.DELIVERY_IN_PROGRESS.equals(order.getStatus())) {
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());

                orderMapper.update(order);
            } else {
                throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
            }
        }
    }

    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.selectById(id);
        if (order == null || !Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", order.getId());
        map.put("content", "订单号：" + order.getNumber());

        webSocketServer.sendToAllClient(JSONObject.toJSONString(map));
    }

    private String getOrderDishesString(Orders order) {
        List<OrderDetail> orderDetails = orderDetailMapper.selectByOrderId(order.getId());
        StrBuilder strBuilder = new StrBuilder();
        for (OrderDetail orderDetail : orderDetails) {
            String s = orderDetail.getName() + "* " + orderDetail.getNumber()+ ";";
            strBuilder.append(s);
        }
        return strBuilder.toString();
    }

}
