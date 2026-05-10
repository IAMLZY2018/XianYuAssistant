package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.TriggerAutoDeliveryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.ManualDeliveryReqDTO;
import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 自动发货控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/autoDelivery")
@CrossOrigin(origins = "*")
public class AutoDeliveryController {

    @Autowired
    private AutoDeliveryService autoDeliveryService;

    /**
     * 触发自动发货
     *
     * @param reqDTO 触发发货请求DTO
     * @return 操作结果
     */
    @PostMapping("/trigger")
    public ResultObject<String> triggerAutoDelivery(@Valid @RequestBody TriggerAutoDeliveryReqDTO reqDTO) {
        try {
            log.info("触发自动发货请求: xianyuAccountId={}, xyGoodsId={}, orderId={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderId());
            return autoDeliveryService.triggerAutoDelivery(reqDTO);
        } catch (Exception e) {
            log.error("触发自动发货失败", e);
            return ResultObject.failed("触发自动发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/manual")
    public ResultObject<String> manualDelivery(@Valid @RequestBody ManualDeliveryReqDTO reqDTO) {
        try {
            log.info("自定义发货请求: xianyuAccountId={}, orderId={}", reqDTO.getXianyuAccountId(), reqDTO.getOrderId());
            return autoDeliveryService.manualDelivery(reqDTO.getXianyuAccountId(), reqDTO.getOrderId(), reqDTO.getContent());
        } catch (Exception e) {
            log.error("自定义发货失败", e);
            return ResultObject.failed("自定义发货失败: " + e.getMessage());
        }
    }
}
