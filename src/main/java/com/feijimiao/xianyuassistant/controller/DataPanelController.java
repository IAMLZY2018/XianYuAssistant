package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.DataPanelStatsRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.DataPanelTrendRespDTO;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @lombok.Data
    public static class StatsReq {
        private String date;
    }
}
