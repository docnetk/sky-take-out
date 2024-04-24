package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据--查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30); //减30天的时间
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        
        BusinessDataVO businessDatavo = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        //2.通过POI将数据写入到Excel文件中
//        InputStream in = this.getClass().getClassLoader().getResourceAsStream("/template/test.xlsx");//在类路径下读取资源返回输入流对象
        InputStream in = null;
        try {

            in = new FileInputStream("D:\\Files\\java\\sky-take-out\\sky-server\\src\\main\\resources\\test.xlsx");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取表格文件的Sheet文件
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDatavo.getTurnover()); //第3个单元格
            row.getCell(4).setCellValue(businessDatavo.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDatavo.getNewUsers());
            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDatavo.getValidOrderCount());
            row.getCell(4).setCellValue(businessDatavo.getUnitPrice());
            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDatavo.getTurnover());
                row.getCell(3).setCellValue(businessDatavo.getValidOrderCount());
                row.getCell(4).setCellValue(businessDatavo.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDatavo.getUnitPrice());
                row.getCell(6).setCellValue(businessDatavo.getNewUsers());
            }
            //3.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
