package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.TriggerAutoDeliveryReqDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;

/**
 * 自动发货服务接口
 */
public interface AutoDeliveryService {
    
    /**
     * 获取商品配置
     */
    XianyuGoodsConfig getGoodsConfig(Long accountId, String xyGoodsId);
    
    /**
     * 获取自动发货配置
     */
    XianyuGoodsAutoDeliveryConfig getAutoDeliveryConfig(Long accountId, String xyGoodsId);
    
    /**
     * 保存或更新商品配置
     */
    void saveOrUpdateGoodsConfig(XianyuGoodsConfig config);
    
    /**
     * 保存或更新自动发货配置
     */
    void saveOrUpdateAutoDeliveryConfig(XianyuGoodsAutoDeliveryConfig config);
    
    /**
     * 记录自动发货
     */
    void recordAutoDelivery(Long accountId, String xyGoodsId, String buyerUserId, String buyerUserName, String content, Integer state);
    
    /**
     * 处理自动发货
     * 当收到"[我已拍下，待付款]"消息时调用
     */
    void handleAutoDelivery(Long accountId, String xyGoodsId, String sId, String buyerUserId, String buyerUserName);
    
    /**
     * 处理自动回复
     * 当收到买家消息时调用
     */
    void handleAutoReply(Long accountId, String xyGoodsId, String sId, String buyerMessage);
    
    /**
     * 获取自动发货记录（分页）
     */
    com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO getAutoDeliveryRecords(
            com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordReqDTO reqDTO);

    /**
     * 触发自动发货
     *
     * @param reqDTO 触发发货请求DTO
     * @return 操作结果
     */
    ResultObject<String> triggerAutoDelivery(TriggerAutoDeliveryReqDTO reqDTO);
}
