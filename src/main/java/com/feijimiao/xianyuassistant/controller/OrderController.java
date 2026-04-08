package com.feijimiao.xianyuassistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.OrderQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.service.OrderService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 确认发货
     */
    @PostMapping("/confirmShipment")
    public ResultObject<String> confirmShipment(@RequestBody ConfirmShipmentReqDTO reqDTO) {
        try {
            log.info("确认发货请求: xianyuAccountId={}, orderId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getOrderId());

            // 参数校验
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            if (reqDTO.getOrderId() == null || reqDTO.getOrderId().isEmpty()) {
                return ResultObject.failed("订单ID不能为空");
            }

            // 确认发货
            String result = orderService.confirmShipment(
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getOrderId()
            );

            if (result != null) {
                return ResultObject.success(result);
            } else {
                return ResultObject.failed("确认发货失败");
            }

        } catch (Exception e) {
            log.error("确认发货失败", e);
            return ResultObject.failed("确认发货失败: " + e.getMessage());
        }
    }

    /**
     * 确认发货请求DTO
     */
    @Data
    public static class ConfirmShipmentReqDTO {
        private Long xianyuAccountId;  // 账号ID
        private String orderId;         // 订单ID
    }
    
    /**
     * 分页查询订单列表
     */
    @PostMapping("/list")
    public ResultObject<Page<OrderVO>> queryOrderList(@RequestBody OrderQueryReqDTO reqDTO) {
        try {
            log.info("查询订单列表请求: xianyuAccountId={}, xyGoodsId={}, orderStatus={}, pageNum={}, pageSize={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderStatus(),
                    reqDTO.getPageNum(), reqDTO.getPageSize());
            
            Page<OrderVO> result = orderService.queryOrderList(reqDTO);
            return ResultObject.success(result);
            
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return ResultObject.failed("查询订单列表失败: " + e.getMessage());
        }
    }
}
