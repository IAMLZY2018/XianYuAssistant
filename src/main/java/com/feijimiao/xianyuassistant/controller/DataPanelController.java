package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.DataPanelStatsRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.DataPanelTrendRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.SalesRevenueRespDTO;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/data-panel")
@CrossOrigin(origins = "*")
public class DataPanelController {

    @Autowired
    private XianyuGoodsOrderMapper orderMapper;

    @Autowired
    private XianyuGoodsAutoReplyRecordMapper replyRecordMapper;

    @PostMapping("/stats")
    public ResultObject<DataPanelStatsRespDTO> getDataPanelStats(@RequestBody(required = false) StatsReq req) {
        try {
            String date;
            if (req != null && req.getDate() != null && !req.getDate().isEmpty()) {
                date = req.getDate();
            } else {
                date = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            }

            DataPanelStatsRespDTO respDTO = new DataPanelStatsRespDTO();
            respDTO.setOrderCount(orderMapper.countOrdersByDate(date));
            respDTO.setDeliverySuccessCount(orderMapper.countDeliverySuccessByDate(date));
            respDTO.setDeliveryFailCount(orderMapper.countDeliveryFailByDate(date));
            respDTO.setAiReplyCount(replyRecordMapper.countAiRepliesByDate(date));
            respDTO.setHasData(orderMapper.countAllOrders() > 0 || replyRecordMapper.countAllReplies() > 0);

            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取数据面板统计失败", e);
            return ResultObject.failed("获取数据面板统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/trend")
    public ResultObject<DataPanelTrendRespDTO> getDataPanelTrend() {
        try {
            DataPanelTrendRespDTO respDTO = new DataPanelTrendRespDTO();
            List<String> dates = new ArrayList<>();
            List<Integer> deliverySuccess = new ArrayList<>();
            List<Integer> deliveryFail = new ArrayList<>();
            List<Integer> aiReplies = new ArrayList<>();

            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

            for (int i = 7; i >= 1; i--) {
                LocalDate d = today.minusDays(i);
                String dateStr = d.format(fmt);
                dates.add(d.getMonthValue() + "/" + d.getDayOfMonth());
                deliverySuccess.add(orderMapper.countDeliverySuccessByDate(dateStr));
                deliveryFail.add(orderMapper.countDeliveryFailByDate(dateStr));
                aiReplies.add(replyRecordMapper.countAiRepliesByDate(dateStr));
            }

            respDTO.setDates(dates);
            respDTO.setDeliverySuccess(deliverySuccess);
            respDTO.setDeliveryFail(deliveryFail);
            respDTO.setAiReplies(aiReplies);

            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取数据面板趋势失败", e);
            return ResultObject.failed("获取数据面板趋势失败: " + e.getMessage());
        }
    }

    @GetMapping("/realtimeRevenue")
    public ResultObject<Double> getRealtimeRevenue() {
        try {
            double amount = orderMapper.sumDeliverySuccessAmount();
            return ResultObject.success(amount);
        } catch (Exception e) {
            log.error("获取实时销售额失败", e);
            return ResultObject.failed("获取实时销售额失败: " + e.getMessage());
        }
    }

    @lombok.Data
    public static class StatsReq {
        private String date;
    }

    @PostMapping("/salesRevenue")
    public ResultObject<SalesRevenueRespDTO> getSalesRevenue(@RequestBody SalesRevenueReq req) {
        try {
            String dimension = req.getDimension() != null ? req.getDimension() : "day";
            String startDateStr = req.getStartDate();
            String endDateStr = req.getEndDate();

            LocalDate endDate;
            LocalDate startDate;
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

            if (endDateStr != null && !endDateStr.isEmpty()) {
                endDate = LocalDate.parse(endDateStr, fmt);
            } else {
                endDate = LocalDate.now().minusDays(1);
            }

            if (startDateStr != null && !startDateStr.isEmpty()) {
                startDate = LocalDate.parse(startDateStr, fmt);
            } else {
                startDate = endDate.minusDays(9);
            }

            SalesRevenueRespDTO respDTO = new SalesRevenueRespDTO();
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();

            switch (dimension) {
                case "week":
                    buildWeeklyData(startDate, endDate, labels, values);
                    break;
                case "month":
                    buildMonthlyData(startDate, endDate, labels, values);
                    break;
                case "quarter":
                    buildQuarterlyData(startDate, endDate, labels, values);
                    break;
                default:
                    buildDailyData(startDate, endDate, labels, values);
                    break;
            }

            respDTO.setLabels(labels);
            respDTO.setValues(values);
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取销售额趋势失败", e);
            return ResultObject.failed("获取销售额趋势失败: " + e.getMessage());
        }
    }

    private void buildDailyData(LocalDate startDate, LocalDate endDate, List<String> labels, List<Double> values) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("M/d");
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            labels.add(d.format(labelFmt));
            values.add(orderMapper.sumDeliverySuccessAmountByDate(d.format(fmt)));
        }
    }

    private void buildWeeklyData(LocalDate startDate, LocalDate endDate, List<String> labels, List<Double> values) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate weekStart = startDate;
        int weekNum = 1;
        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) weekEnd = endDate;
            labels.add("第" + weekNum + "周");
            values.add(orderMapper.sumDeliverySuccessAmountByDateRange(weekStart.format(fmt), weekEnd.format(fmt)));
            weekStart = weekEnd.plusDays(1);
            weekNum++;
        }
    }

    private void buildMonthlyData(LocalDate startDate, LocalDate endDate, List<String> labels, List<Double> values) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate monthStart = startDate.withDayOfMonth(1);
        while (!monthStart.isAfter(endDate)) {
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(endDate)) monthEnd = endDate;
            if (monthStart.isBefore(startDate)) monthStart = startDate;
            labels.add(monthStart.getYear() + "/" + monthStart.getMonthValue());
            values.add(orderMapper.sumDeliverySuccessAmountByDateRange(monthStart.format(fmt), monthEnd.format(fmt)));
            monthStart = monthStart.plusMonths(1).withDayOfMonth(1);
        }
    }

    private void buildQuarterlyData(LocalDate startDate, LocalDate endDate, List<String> labels, List<Double> values) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        int startQuarter = (startDate.getMonthValue() - 1) / 3 + 1;
        LocalDate quarterStart = startDate.withMonth((startQuarter - 1) * 3 + 1).withDayOfMonth(1);
        while (!quarterStart.isAfter(endDate)) {
            int quarter = (quarterStart.getMonthValue() - 1) / 3 + 1;
            LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
            if (quarterEnd.isAfter(endDate)) quarterEnd = endDate;
            LocalDate actualStart = quarterStart.isBefore(startDate) ? startDate : quarterStart;
            labels.add(quarterStart.getYear() + "Q" + quarter);
            values.add(orderMapper.sumDeliverySuccessAmountByDateRange(actualStart.format(fmt), quarterEnd.format(fmt)));
            quarterStart = quarterStart.plusMonths(3);
        }
    }

    @lombok.Data
    public static class SalesRevenueReq {
        private String dimension;
        private String startDate;
        private String endDate;
    }
}
