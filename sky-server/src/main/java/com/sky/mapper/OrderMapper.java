package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into orders(number, status, user_id, address_book_id, order_time, checkout_time, pay_method, pay_status, amount, remark, phone, address, user_name, consignee, cancel_reason, rejection_reason, cancel_time, estimated_delivery_time, delivery_status, delivery_time, pack_amount, tableware_number, tableware_status) " +
            "VALUES (#{number}, #{status}, #{userId}, #{addressBookId}, #{orderTime}, #{checkoutTime}, #{payMethod}, #{payStatus}, #{amount}, #{remark}, #{phone}, #{address}, #{userName}, #{consignee}, #{cancelReason}, #{rejectionReason}, #{cancelTime}, #{estimatedDeliveryTime}, #{deliveryStatus}, #{deliveryTime}, #{packAmount}, #{tablewareNumber}, #{tablewareStatus})")
    void insert(Orders order);

    @Select("select * from orders where number=#{outTradeNo};")
    Orders getByNumber(String outTradeNo);

    void update(Orders orders);

    void updateStatus(Orders orders);

    /*
    根据用户Id和订单状态查询历史订单
     */
    Page<Orders> queryOrders(OrdersPageQueryDTO dto);

    @Select("select * from orders where id=#{id}")
    Orders selectById(Long id);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where status=#{status}")
    List<Orders> selectByStatus(Integer status);
}
