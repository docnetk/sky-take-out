package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    public Result<PageResult> searchOrder(OrdersPageQueryDTO ordersPaymentDTO) {
        PageResult pageResult = orderService.searchOrders(ordersPaymentDTO);
        return Result.success(pageResult);
    }

    @GetMapping("statistics")
    public Result<OrderStatisticsVO> statisticsOrder() {
        OrderStatisticsVO statisticsVO = orderService.statisticsOrder();
        return Result.success(statisticsVO);
    }

    @GetMapping("details/{id}")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        OrderVO orderVO = orderService.orderDetail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/confirm")
    public Result<Object> receiveOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        orderService.receiveOrder(ordersConfirmDTO.getId());
        return Result.success();
    }

    @PutMapping("/rejection")
    public Result<Object> rejectOrder(@RequestBody OrdersRejectionDTO rejectionDTO) {
        orderService.rejectOrder(rejectionDTO);
        return Result.success();
    }

    @PutMapping("/cancel")
    public Result<Object> cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) {
        orderService.cancelOrderByAdmin(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    public Result<Object> delivery(@PathVariable Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result<Object> completeOrder(@PathVariable Long id) {
        orderService.completeOrder(id);
        return Result.success();
    }
}
