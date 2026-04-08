package com.feijimiao.xianyuassistant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.feijimiao.xianyuassistant.controller.dto.OrderQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.entity.XianyuOrder;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 保存或更新订单
     * 
     * @param order 订单信息
     * @return 订单ID
     */
    Long saveOrUpdateOrder(XianyuOrder order);
    
    /**
     * 根据订单ID查询订单
     * 
     * @param orderId 订单ID
     * @return 订单信息
     */
    XianyuOrder getOrderByOrderId(String orderId);
    
    /**
     * 根据账号ID和订单ID查询订单
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     * @return 订单信息
     */
    XianyuOrder getOrderByAccountIdAndOrderId(Long accountId, String orderId);
    
    /**
     * 确认发货
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     * @return 操作结果
     */
    String confirmShipment(Long accountId, String orderId);
    
    /**
     * 调用闲鱼API确认发货
     * 先调用闲鱼API确认发货,成功后再更新本地数据库
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     * @return 操作结果
     */
    String confirmShipmentToXianyu(Long accountId, String orderId);
    
    /**
     * 分页查询订单列表
     * 
     * @param reqDTO 查询条件
     * @return 分页结果
     */
    Page<OrderVO> queryOrderList(OrderQueryReqDTO reqDTO);
}
