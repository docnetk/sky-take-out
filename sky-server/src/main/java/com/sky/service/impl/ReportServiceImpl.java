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
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> amounts = new ArrayList<>();
        for (LocalDate t : dateList) {
            LocalDateTime a = LocalDateTime.of(t, LocalTime.MIN);
            LocalDateTime b = LocalDateTime.of(t, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", a);
            map.put("end", b);
            map.put("status", Orders.COMPLETED);
            Double amount = orderMapper.turnover(map);
            if (amount == null) {
                amount = 0.0;
            }
            amounts.add(amount);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(amounts, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Long> nums1 = new ArrayList<>(), nums2 = new ArrayList<>();
        for (LocalDate t : dateList) {
            LocalDateTime a = LocalDateTime.of(t, LocalTime.MIN);
            LocalDateTime b = LocalDateTime.of(t, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("end", b);
            long allUser = userMapper.getCount(map);

            map.put("begin", a);
            long newUser = userMapper.getCount(map);

            nums1.add(allUser);
            nums2.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(nums2, ","))
                .totalUserList(StringUtils.join(nums1, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        int totalOrder = 0, validOrder = 0;
        List<Integer> totalList = new ArrayList<>(), validList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime a = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime b = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", a);
            map.put("end", b);
            Integer totalNum = orderMapper.countOrder(map);
            totalList.add(totalNum);
            totalOrder += totalNum;

            map.put("status", Orders.COMPLETED);
            Integer validNum = orderMapper.countOrder(map);
            validList.add(validNum);
            validOrder += validNum;
        }
        double rate = 0.0;
        if (totalOrder != 0) {
            rate = validOrder * 1.0 / totalOrder;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(totalList, ","))
                .validOrderCountList(StringUtils.join(validList, ","))
                .totalOrderCount(totalOrder)
                .validOrderCount(validOrder)
                .orderCompletionRate(rate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime pre = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime next = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(pre, next);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }
}
