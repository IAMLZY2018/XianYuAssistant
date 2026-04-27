package com.feijimiao.xianyuassistant.service;

/**
 * 订单服务接口
 */
public interface OrderService {
    
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
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     * @return 操作结果
     */
    String confirmShipmentToXianyu(Long accountId, String orderId);
}
