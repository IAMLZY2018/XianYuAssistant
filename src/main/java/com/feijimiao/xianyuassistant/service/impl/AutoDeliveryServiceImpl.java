package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.entity.XianyuKamiUsageRecord;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiUsageRecordMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import com.feijimiao.xianyuassistant.service.EmailNotifyService;
import com.feijimiao.xianyuassistant.service.KamiConfigService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自动发货服务实现类
 */
@Slf4j
@Service
public class AutoDeliveryServiceImpl implements AutoDeliveryService {
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;
    
    @Autowired
    private XianyuGoodsOrderMapper orderMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyRecordMapper autoReplyRecordMapper;
    
    @Lazy
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.SentMessageSaveService sentMessageSaveService;

    @Autowired
    private KamiConfigService kamiConfigService;

    @Autowired
    private EmailNotifyService emailNotifyService;

    @Autowired
    private XianyuKamiUsageRecordMapper kamiUsageRecordMapper;

    @Autowired
    private OrderService orderService;
    
    @Override
    public XianyuGoodsConfig getGoodsConfig(Long accountId, String xyGoodsId) {
        return goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
    }
    
    @Override
    public XianyuGoodsAutoDeliveryConfig getAutoDeliveryConfig(Long accountId, String xyGoodsId) {
        return autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
    }
    
    @Override
    public void saveOrUpdateGoodsConfig(XianyuGoodsConfig config) {
        XianyuGoodsConfig existing = goodsConfigMapper.selectByAccountAndGoodsId(
                config.getXianyuAccountId(), config.getXyGoodsId());
        
        if (existing == null) {
            goodsConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            goodsConfigMapper.update(config);
        }
    }
    
    @Override
    public void saveOrUpdateAutoDeliveryConfig(XianyuGoodsAutoDeliveryConfig config) {
        XianyuGoodsAutoDeliveryConfig existing = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(
                config.getXianyuAccountId(), config.getXyGoodsId());
        
        if (existing == null) {
            autoDeliveryConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            autoDeliveryConfigMapper.updateById(config);
        }
    }
    
    @Override
    public void recordAutoDelivery(Long accountId, String xyGoodsId, String buyerUserId, String buyerUserName, String content, Integer state) {
        // 使用新的重载方法，传入 null 作为 pnmId 和 orderId
        recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, content, state, null, null);
    }
    
    /**
     * 记录自动发货（带 pnmId 和 orderId）
     */
    public void recordAutoDelivery(Long accountId, String xyGoodsId, String buyerUserId, String buyerUserName, 
                                   String content, Integer state, String pnmId, String orderId) {
        XianyuGoodsOrder record = new XianyuGoodsOrder();
        record.setXianyuAccountId(accountId);
        record.setXyGoodsId(xyGoodsId);
        record.setBuyerUserId(buyerUserId);
        record.setBuyerUserName(buyerUserName);
        record.setContent(content);
        record.setState(state);
        record.setPnmId(pnmId != null ? pnmId : "");
        record.setOrderId(orderId != null ? orderId : "");
        record.setConfirmState(0);
        
        orderMapper.insert(record);
    }
    
    /**
     * 处理自动发货（带买家用户ID和用户名）
     */
    @Override
    public void handleAutoDelivery(Long accountId, String xyGoodsId, String sId, String buyerUserId, String buyerUserName) {
        // 调用重载方法，传入 null 作为 orderId
        handleAutoDelivery(accountId, xyGoodsId, sId, buyerUserId, buyerUserName, null);
    }
    
    /**
     * 处理自动发货（带订单ID）
     */
    public void handleAutoDelivery(Long accountId, String xyGoodsId, String sId, String buyerUserId, String buyerUserName, String orderId) {
        try {
            log.info("【账号{}】处理自动发货: xyGoodsId={}, sId={}, buyerUserId={}, buyerUserName={}, orderId={}", 
                    accountId, xyGoodsId, sId, buyerUserId, buyerUserName, orderId);
            
            // 1. 检查商品是否开启自动发货
            XianyuGoodsConfig goodsConfig = getGoodsConfig(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 2. 获取自动发货配置
            XianyuGoodsAutoDeliveryConfig deliveryConfig = getAutoDeliveryConfig(accountId, xyGoodsId);
            if (deliveryConfig == null || deliveryConfig.getAutoDeliveryContent() == null || 
                    deliveryConfig.getAutoDeliveryContent().isEmpty()) {
                log.warn("【账号{}】商品未配置自动发货内容: xyGoodsId={}", accountId, xyGoodsId);
                recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, null, 0, null, orderId);
                return;
            }
            
            String content = deliveryConfig.getAutoDeliveryContent();
            log.info("【账号{}】准备发送自动发货消息: content={}", accountId, content);

            // 3. 模拟人工操作：阅读消息 + 思考 + 打字延迟

            // 3.1 阅读买家消息的延迟（1-3秒）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.mediumDelay();

            // 3.2 思考延迟（1-4秒）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.thinkingDelay();

            // 3.3 打字延迟（根据内容长度）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.typingDelay(content.length());
            
            // 4. 从sId中提取cid和toId
            // sId格式: "55435931514@goofish"
            String cid = sId.replace("@goofish", "");
            String toId = cid; // 对于闲鱼，cid和toId通常相同
            
            // 5. 发送消息
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);
            
            // 6. 记录发货结果（传递买家用户ID和用户名、orderId）
            recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, content, success ? 1 : 0, null, orderId);
            
            if (success) {
                log.info("【账号{}】自动发货成功: xyGoodsId={}, buyerUserName={}, content={}", 
                        accountId, xyGoodsId, buyerUserName, content);
                // 自动发货消息入库（contentType=888，AI助手回复）
                sentMessageSaveService.saveAiAssistantReply(accountId, cid, toId, content, xyGoodsId);
            } else {
                log.error("【账号{}】自动发货失败: xyGoodsId={}", accountId, xyGoodsId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】自动发货异常: xyGoodsId={}", accountId, xyGoodsId, e);
            recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, null, 0, null, orderId);
        }
    }
    
    @Override
    public void handleAutoReply(Long accountId, String xyGoodsId, String sId, String buyerMessage) {
        log.info("【账号{}】自动回复功能已移除: xyGoodsId={}", accountId, xyGoodsId);
    }
    
    private void recordAutoReply(Long accountId, String xyGoodsId, String buyerMessage, 
                                  String replyContent, String matchedKeyword, Integer state) {
        try {
            XianyuGoodsAutoReplyRecord record = new XianyuGoodsAutoReplyRecord();
            record.setXianyuAccountId(accountId);
            record.setXyGoodsId(xyGoodsId);
            record.setBuyerMessage(buyerMessage);
            record.setReplyContent(replyContent);
            record.setMatchedKeyword(matchedKeyword);
            record.setState(state);
            
            autoReplyRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("【账号{}】记录自动回复失败", accountId, e);
        }
    }
    
    @Override
    public com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO getAutoDeliveryRecords(
            com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordReqDTO reqDTO) {
        
        Long accountId = reqDTO.getXianyuAccountId();
        String xyGoodsId = reqDTO.getXyGoodsId();
        int pageNum = reqDTO.getPageNum() != null ? reqDTO.getPageNum() : 1;
        int pageSize = reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20;
        
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        // 查询记录
        List<XianyuGoodsOrder> records = orderMapper.selectByAccountIdWithPage(
                accountId, xyGoodsId, pageSize, offset);
        
        // 统计总数
        long total = orderMapper.countByAccountId(accountId, xyGoodsId);
        
        // 转换为DTO
        List<com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO> recordDTOs = new ArrayList<>();
        for (XianyuGoodsOrder record : records) {
            com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO dto = 
                    new com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO();
            dto.setId(record.getId());
            dto.setXyGoodsId(record.getXyGoodsId());
            dto.setGoodsTitle(record.getGoodsTitle());
            dto.setBuyerUserName(record.getBuyerUserName());
            dto.setContent(record.getContent());
            dto.setState(record.getState());
            dto.setConfirmState(record.getConfirmState());
            dto.setOrderId(record.getOrderId());
            dto.setCreateTime(record.getCreateTime());
            recordDTOs.add(dto);
        }
        
        // 构建响应
        com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO respDTO = 
                new com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO();
        respDTO.setRecords(recordDTOs);
        respDTO.setTotal(total);
        respDTO.setPageNum(pageNum);
        respDTO.setPageSize(pageSize);
        
        return respDTO;
    }

    @Override
    public com.feijimiao.xianyuassistant.common.ResultObject<String> triggerAutoDelivery(
            com.feijimiao.xianyuassistant.controller.dto.TriggerAutoDeliveryReqDTO reqDTO) {
        try {
            Long accountId = reqDTO.getXianyuAccountId();
            String xyGoodsId = reqDTO.getXyGoodsId();
            String orderId = reqDTO.getOrderId();
            Boolean needHumanLikeDelay = reqDTO.getNeedHumanLikeDelay() != null ? reqDTO.getNeedHumanLikeDelay() : false;

            log.info("【账号{}】触发自动发货: xyGoodsId={}, orderId={}, needHumanLikeDelay={}", 
                    accountId, xyGoodsId, orderId, needHumanLikeDelay);

            XianyuGoodsOrder record = orderMapper.selectByOrderId(accountId, xyGoodsId, orderId);
            if (record == null) {
                log.warn("【账号{}】发货记录不存在: orderId={}", accountId, orderId);
                return com.feijimiao.xianyuassistant.common.ResultObject.failed("发货记录不存在");
            }

            String pnmId = record.getPnmId();
            if (pnmId == null || pnmId.isEmpty()) {
                log.warn("【账号{}】发货记录没有pnmId: orderId={}", accountId, orderId);
                return com.feijimiao.xianyuassistant.common.ResultObject.failed("发货记录没有pnmId");
            }

            Long recordId = record.getId();
            String sId = record.getSid() != null ? record.getSid() : record.getBuyerUserId() + "@goofish";
            String buyerUserName = record.getBuyerUserName();

            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
                return com.feijimiao.xianyuassistant.common.ResultObject.failed("商品未开启自动发货");
            }

            executeDelivery(recordId, accountId, xyGoodsId, sId, orderId, buyerUserName, needHumanLikeDelay);

            XianyuGoodsOrder updatedRecord = orderMapper.selectByOrderId(accountId, xyGoodsId, orderId);
            if (updatedRecord != null && updatedRecord.getState() == 1) {
                return com.feijimiao.xianyuassistant.common.ResultObject.success("触发自动发货成功");
            } else {
                String failReason = updatedRecord != null ? updatedRecord.getFailReason() : "未知错误";
                return com.feijimiao.xianyuassistant.common.ResultObject.failed(failReason != null ? failReason : "发货失败");
            }

        } catch (Exception e) {
            log.error("【账号{}】触发自动发货失败: xyGoodsId={}, orderId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderId(), e);
            return com.feijimiao.xianyuassistant.common.ResultObject.failed("触发自动发货失败: " + e.getMessage());
        }
    }

    @Override
    public void executeDelivery(Long recordId, Long accountId, String xyGoodsId, String sId, String orderId, String buyerUserName, boolean needHumanLikeDelay) {
        try {
            log.info("【账号{}】开始执行自动发货: recordId={}, xyGoodsId={}, orderId={}", accountId, recordId, xyGoodsId, orderId);

            XianyuGoodsAutoDeliveryConfig deliveryConfig = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
            if (deliveryConfig == null) {
                log.warn("【账号{}】商品未配置发货模式: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, -1, null, "商品未配置发货模式");
                emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, "商品未配置发货模式");
                return;
            }

            int deliveryMode = deliveryConfig.getDeliveryMode() != null ? deliveryConfig.getDeliveryMode() : 1;
            String content = resolveDeliveryContent(deliveryMode, deliveryConfig, recordId, accountId, xyGoodsId, sId, orderId, buyerUserName);
            if (content == null) {
                return;
            }

            if (needHumanLikeDelay) {
                log.debug("【账号{}】模拟人工操作延迟...", accountId);
                HumanLikeDelayUtils.mediumDelay();
                HumanLikeDelayUtils.thinkingDelay();
                HumanLikeDelayUtils.typingDelay(content.length());
            }

            String cid = sId.replace("@goofish", "");
            String toId = cid;

            sendDeliveryImages(accountId, xyGoodsId, cid, toId, deliveryConfig, needHumanLikeDelay);

            log.info("【账号{}】准备发送发货文本: content长度={}, deliveryMode={}", accountId, content.length(), deliveryMode);
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);

            if (success) {
                log.info("【账号{}】✅ 自动发货成功: recordId={}, xyGoodsId={}, deliveryMode={}", accountId, recordId, xyGoodsId, deliveryMode);
                updateRecordState(recordId, 1, content, null);
                sentMessageSaveService.saveAiAssistantReply(accountId, cid, toId, content, xyGoodsId);

                if (deliveryConfig.getAutoConfirmShipment() != null && deliveryConfig.getAutoConfirmShipment() == 1) {
                    log.info("【账号{}】检测到自动确认发货开关已开启，准备自动确认发货: orderId={}", accountId, orderId);
                    executeAutoConfirmShipment(accountId, orderId);
                }
            } else {
                log.error("【账号{}】❌ 自动发货失败(服务端拒绝): recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId);
                updateRecordState(recordId, -1, content, "消息发送失败");
                emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, "消息发送失败");
            }

        } catch (Exception e) {
            log.error("【账号{}】执行自动发货异常: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId, e);
            String failReason = "发货异常: " + e.getMessage();
            updateRecordState(recordId, -1, null, failReason);
            emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, failReason);
        }
    }

    private String resolveDeliveryContent(int deliveryMode, XianyuGoodsAutoDeliveryConfig deliveryConfig,
                                          Long recordId, Long accountId, String xyGoodsId, String sId, String orderId, String buyerUserName) {
        if (deliveryMode == 1) {
            if (deliveryConfig.getAutoDeliveryContent() == null || deliveryConfig.getAutoDeliveryContent().isEmpty()) {
                log.warn("【账号{}】自动发货模式下未配置发货内容: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, -1, null, "未配置发货内容");
                emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, "未配置发货内容");
                return null;
            }
            log.info("【账号{}】文本发货模式", accountId);
            return deliveryConfig.getAutoDeliveryContent();
        } else if (deliveryMode == 2) {
            String content = acquireKamiContent(deliveryConfig.getKamiConfigIds(), deliveryConfig.getKamiDeliveryTemplate(), orderId, accountId, xyGoodsId, sId, buyerUserName);
            if (content == null) {
                log.warn("【账号{}】卡密发货模式下无可用卡密: xyGoodsId={}, kamiConfigIds={}", accountId, xyGoodsId, deliveryConfig.getKamiConfigIds());
                updateRecordState(recordId, -1, null, "卡密库存不足，无可用卡密");
                emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, "卡密库存不足，无可用卡密");
                return null;
            }
            log.info("【账号{}】卡密发货模式: content长度={}", accountId, content.length());
            return content;
        } else if (deliveryMode == 3) {
            log.info("【账号{}】自定义发货模式，不自动发送消息: xyGoodsId={}", accountId, xyGoodsId);
            updateRecordState(recordId, 1, "自定义发货-请通过API处理", null);
            return null;
        } else {
            log.warn("【账号{}】未知的发货模式: deliveryMode={}", accountId, deliveryMode);
            updateRecordState(recordId, -1, null, "未知的发货模式: " + deliveryMode);
            emailNotifyService.sendAutoDeliveryFailEmail(null, xyGoodsId, orderId, "未知的发货模式: " + deliveryMode);
            return null;
        }
    }

    private void sendDeliveryImages(Long accountId, String xyGoodsId, String cid, String toId,
                                    XianyuGoodsAutoDeliveryConfig deliveryConfig, boolean needHumanLikeDelay) {
        String imageUrlStr = deliveryConfig.getAutoDeliveryImageUrl();
        if (imageUrlStr == null || imageUrlStr.trim().isEmpty()) {
            return;
        }
        String[] imageUrls = imageUrlStr.split(",");
        for (int i = 0; i < imageUrls.length; i++) {
            String url = imageUrls[i].trim();
            if (url.isEmpty()) continue;
            if (i > 0) {
                if (needHumanLikeDelay) {
                    HumanLikeDelayUtils.thinkingDelay();
                } else {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }
            boolean imgSuccess = webSocketService.sendImageMessage(accountId, cid, toId, url, 800, 800);
            if (imgSuccess) {
                log.info("【账号{}】自动发货图片[{}/{}]发送成功: xyGoodsId={}", accountId, i + 1, imageUrls.length, xyGoodsId);
                sentMessageSaveService.saveManualImageReply(accountId, cid, toId, url, xyGoodsId);
            } else {
                log.warn("【账号{}】自动发货图片[{}/{}]发送失败: xyGoodsId={}", accountId, i + 1, imageUrls.length, xyGoodsId);
            }
        }
    }

    private void executeAutoConfirmShipment(Long accountId, String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            log.warn("【账号{}】订单ID为空，无法自动确认发货", accountId);
            return;
        }
        log.info("【账号{}】提交异步自动确认发货: orderId={}", accountId, orderId);
        new Thread(() -> {
            try {
                HumanLikeDelayUtils.longDelay();
                String result = orderService.confirmShipment(accountId, orderId);
                if (result != null) {
                    log.info("【账号{}】✅ 自动确认发货成功: orderId={}", accountId, orderId);
                } else {
                    log.error("【账号{}】❌ 自动确认发货失败: orderId={}", accountId, orderId);
                }
            } catch (Exception e) {
                log.error("【账号{}】自动确认发货异常: orderId={}", accountId, orderId, e);
            }
        }).start();
    }

    private void updateRecordState(Long recordId, Integer state, String content, String failReason) {
        try {
            orderMapper.updateStateContentAndFailReason(recordId, state, content, failReason);
        } catch (Exception e) {
            log.error("更新订单状态失败: recordId={}, state={}", recordId, state, e);
        }
    }

    @Override
    public void updateAutoConfirmShipment(Long accountId, String xyGoodsId, Integer autoConfirmShipment) {
        XianyuGoodsAutoDeliveryConfig config = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
        if (config == null) {
            config = new XianyuGoodsAutoDeliveryConfig();
            config.setXianyuAccountId(accountId);
            config.setXyGoodsId(xyGoodsId);
            config.setAutoConfirmShipment(autoConfirmShipment);
            autoDeliveryConfigMapper.insert(config);
        } else {
            config.setAutoConfirmShipment(autoConfirmShipment);
            autoDeliveryConfigMapper.updateById(config);
        }
    }

    private String acquireKamiContent(String kamiConfigIds, String kamiDeliveryTemplate, String orderId, Long accountId, String xyGoodsId, String sId, String buyerUserName) {
        if (kamiConfigIds == null || kamiConfigIds.trim().isEmpty()) {
            log.warn("【账号{}】卡密发货未绑定卡密配置: xyGoodsId={}", accountId, xyGoodsId);
            return null;
        }

        String[] configIdArr = kamiConfigIds.split(",");
        for (String configIdStr : configIdArr) {
            try {
                Long configId = Long.parseLong(configIdStr.trim());
                XianyuKamiItem kamiItem = kamiConfigService.acquireKami(configId, orderId);
                if (kamiItem != null) {
                    XianyuKamiUsageRecord usageRecord = new XianyuKamiUsageRecord();
                    usageRecord.setKamiConfigId(configId);
                    usageRecord.setKamiItemId(kamiItem.getId());
                    usageRecord.setXianyuAccountId(accountId);
                    usageRecord.setXyGoodsId(xyGoodsId);
                    usageRecord.setOrderId(orderId);
                    usageRecord.setKamiContent(kamiItem.getKamiContent());
                    String cid = sId.replace("@goofish", "");
                    usageRecord.setBuyerUserId(cid);
                    usageRecord.setBuyerUserName(buyerUserName);
                    kamiUsageRecordMapper.insert(usageRecord);
                    log.info("【账号{}】卡密扣减成功: configId={}, itemId={}, orderId={}", accountId, configId, kamiItem.getId(), orderId);

                    String kamiContent = kamiItem.getKamiContent();
                    if (kamiDeliveryTemplate != null && !kamiDeliveryTemplate.trim().isEmpty()) {
                        kamiContent = kamiDeliveryTemplate.replace("{kmKey}", kamiContent);
                    }
                    return kamiContent;
                }
            } catch (NumberFormatException e) {
                log.warn("【账号{}】卡密配置ID格式错误: {}", accountId, configIdStr);
            }
        }
        return null;
    }
}
