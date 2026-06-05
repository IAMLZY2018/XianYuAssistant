package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.ConfirmShipmentReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.OrderDetailReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.OrderListReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.OrderListRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.OrderDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import com.feijimiao.xianyuassistant.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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

    @Autowired
    private XianyuGoodsOrderMapper orderMapper;

    @Autowired
    private com.feijimiao.xianyuassistant.service.PendingOrderPollService pendingOrderPollService;

    /**
     * 查询订单列表（第三方调用）
     */
    @PostMapping("/list")
    public ResultObject<OrderListRespDTO> listOrders(@RequestBody OrderListReqDTO reqDTO) {
        try {
            int pageSize = reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20;
            int pageNum = reqDTO.getPageNum() != null ? reqDTO.getPageNum() : 1;
            if (pageNum < 1) pageNum = 1;

            int offset = (pageNum - 1) * pageSize;
            long total = orderMapper.countByCondition(
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderStatus(), reqDTO.getKeyword());

            List<XianyuGoodsOrder> orders = orderMapper.selectByConditionWithPage(
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderStatus(), reqDTO.getKeyword(),
                    pageSize, offset);

            OrderListRespDTO respDTO = new OrderListRespDTO();
            respDTO.setTotal(total);
            respDTO.setPageNum(pageNum);
            respDTO.setPageSize(pageSize);

            List<OrderDTO> orderDTOs = new ArrayList<>();
            for (XianyuGoodsOrder order : orders) {
                OrderDTO dto = new OrderDTO();
                dto.setId(order.getId());
                dto.setXianyuAccountId(order.getXianyuAccountId());
                dto.setXyGoodsId(order.getXyGoodsId());
                dto.setOrderId(order.getOrderId());
                dto.setBuyerUserName(order.getBuyerUserName());
                dto.setSid(order.getSid());
                dto.setContent(order.getContent());
                dto.setState(order.getState());
                dto.setFailReason(order.getFailReason());
                dto.setConfirmState(order.getConfirmState());
                dto.setGoodsTitle(order.getGoodsTitle());
                dto.setSkuName(order.getSkuName());
                dto.setOrderCreateTime(order.getOrderCreateTime());
                dto.setPaySuccessTime(order.getPaySuccessTime());
                dto.setConsignTime(order.getConsignTime());
                dto.setTotalPrice(order.getTotalPrice());
                dto.setBuyNum(order.getBuyNum());
                dto.setCreateTime(order.getCreateTime());
                orderDTOs.add(dto);
            }
            respDTO.setRecords(orderDTOs);

            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return ResultObject.failed("查询订单列表失败: " + e.getMessage());
        }
    }

    /**
     * 确认发货
     */
    @PostMapping("/confirmShipment")
    public ResultObject<String> confirmShipment(@RequestBody ConfirmShipmentReqDTO reqDTO) {
        try {
            log.info("确认发货请求: xianyuAccountId={}, orderId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getOrderId());

            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            if (reqDTO.getOrderId() == null || reqDTO.getOrderId().isEmpty()) {
                return ResultObject.failed("订单ID不能为空");
            }

            String result = orderService.confirmShipment(
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getOrderId()
            );

            if (result != null) {
                orderMapper.updateConfirmState(reqDTO.getXianyuAccountId(), reqDTO.getOrderId());
                return ResultObject.success(result);
            } else {
                return ResultObject.failed("确认发货失败");
            }

        } catch (Exception e) {
            log.error("确认发货失败", e);
            return ResultObject.failed("确认发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/detail")
    public ResultObject<String> getOrderDetail(@RequestBody OrderDetailReqDTO reqDTO) {
        try {
            log.info("获取订单详情请求: xianyuAccountId={}, orderId={}, fromServer={}",
                    reqDTO.getXianyuAccountId(), reqDTO.getOrderId(), reqDTO.getFromServer());

            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            if (reqDTO.getOrderId() == null || reqDTO.getOrderId().isEmpty()) {
                return ResultObject.failed("订单ID不能为空");
            }

            boolean fromServer = reqDTO.getFromServer() != null && reqDTO.getFromServer();
            String detail;
            if (fromServer) {
                detail = orderService.getOrderDetail(reqDTO.getXianyuAccountId(), reqDTO.getOrderId());
            } else {
                detail = orderService.getOrderDetailFromLocal(reqDTO.getXianyuAccountId(), reqDTO.getOrderId());
            }
            if (detail != null) {
                return ResultObject.success(detail);
            } else {
                return ResultObject.failed("获取订单详情失败");
            }
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return ResultObject.failed("获取订单详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/pendingOrders")
    public ResultObject<List<Map<String, Object>>> getPendingOrders(@RequestBody PendingOrdersReqDTO reqDTO) {
        try {
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            List<Map<String, Object>> orders = orderService.queryPendingOrders(reqDTO.getXianyuAccountId());
            pendingOrderPollService.syncOrdersToDb(reqDTO.getXianyuAccountId(), orders);
            return ResultObject.success(orders);
        } catch (Exception e) {
            log.error("查询待发货订单失败", e);
            return ResultObject.failed("查询待发货订单失败: " + e.getMessage());
        }
    }

    @lombok.Data
    public static class PendingOrdersReqDTO {
        private Long xianyuAccountId;
    }

    @PostMapping("/deliverPendingOrders")
    public ResultObject<Integer> deliverPendingOrders(@RequestBody PendingOrdersReqDTO reqDTO) {
        try {
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            int count = pendingOrderPollService.deliverPendingOrders(reqDTO.getXianyuAccountId());
            return ResultObject.success(count);
        } catch (Exception e) {
            log.error("自动发货失败", e);
            return ResultObject.failed("自动发货失败: " + e.getMessage());
        }
    }

    @PostMapping("/consignDummyDelivery")
    public ResultObject<String> consignDummyDelivery(@RequestBody ConsignDummyReqDTO reqDTO) {
        try {
            log.info("凭证发货请求: xianyuAccountId={}, xyGoodsId={}, orderId={}", reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderId());
            if (reqDTO.getXianyuAccountId() == null) {
                return ResultObject.failed("账号ID不能为空");
            }
            if (reqDTO.getOrderId() == null || reqDTO.getOrderId().isEmpty()) {
                return ResultObject.failed("订单ID不能为空");
            }
            if (reqDTO.getXyGoodsId() == null || reqDTO.getXyGoodsId().isEmpty()) {
                return ResultObject.failed("商品ID不能为空");
            }
            String result = orderService.consignDummyDeliveryWithConfig(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderId());
            if (result != null) {
                log.info("凭证发货成功: xianyuAccountId={}, orderId={}, result={}", reqDTO.getXianyuAccountId(), reqDTO.getOrderId(), result);
                return ResultObject.success(result);
            } else {
                log.error("凭证发货失败: xianyuAccountId={}, orderId={}", reqDTO.getXianyuAccountId(), reqDTO.getOrderId());
                return ResultObject.failed("凭证发货失败");
            }
        } catch (Exception e) {
            log.error("凭证发货异常: xianyuAccountId={}, orderId={}", reqDTO.getXianyuAccountId(), reqDTO.getOrderId(), e);
            return ResultObject.failed("凭证发货失败: " + e.getMessage());
        }
    }

    @lombok.Data
    public static class ConsignDummyReqDTO {
        private Long xianyuAccountId;
        private String xyGoodsId;
        private String orderId;
    }
}
