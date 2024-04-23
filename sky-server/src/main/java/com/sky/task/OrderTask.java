package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTask {
    private final String CANCEL_REASON = "用户支付超时，自动取消订单";

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 0/1 * * * ? ")
    public void timeOutOrder() {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(-15);
        System.out.println(localDateTime);
        List<Orders> orders = orderMapper.selectByCondition(Orders.PENDING_PAYMENT, localDateTime);

        if (orders == null) {
            return;
        }
        for (Orders order : orders) {
            order.setStatus(Orders.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason(CANCEL_REASON);

            orderMapper.update(order);
        }
    }

    @Scheduled(cron = "0 0 1 * * ? ")
    public void deliveryOrder() {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(-60);
        List<Orders> orders = orderMapper.selectByCondition(Orders.DELIVERY_IN_PROGRESS, localDateTime);

        if (orders == null) {
            return;
        }
        for (Orders order : orders) {
            order.setStatus(Orders.COMPLETED);

            orderMapper.update(order);
        }
    }
}
