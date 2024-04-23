package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import java.util.List;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult getHistoricalOrders(int page, int pageSize, Integer status);

    OrderVO orderDetail(Long id);

    void cancelOrder(Long id);

    void repetition(Long id);

    PageResult searchOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statisticsOrder();

    void receiveOrder(Long id);

    void rejectOrder(OrdersRejectionDTO rejectionDTO);

    void cancelOrderByAdmin(OrdersCancelDTO ordersCancelDTO);

    void delivery(Long id);

    void completeOrder(Long id);

    void reminder(Long id);
}
