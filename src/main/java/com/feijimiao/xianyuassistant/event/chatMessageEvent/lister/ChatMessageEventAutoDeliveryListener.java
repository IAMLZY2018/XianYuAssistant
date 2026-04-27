package com.feijimiao.xianyuassistant.event.chatMessageEvent.lister;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageReceivedEvent;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils;
import com.feijimiao.xianyuassistant.service.KamiConfigService;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiUsageRecordMapper;
import com.feijimiao.xianyuassistant.entity.XianyuKamiUsageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 自动发货用
 * 
 * <p>监听 {@link ChatMessageReceivedEvent} 事件，判断是否需要触发自动发货</p>
 * 
 * <p>触发条件：</p>
 * <ul>
 *   <li>contentType = 32（已付款待发货类型）</li>
 *   <li>msgContent 包含 "[已付款，待发货]"</li>
 * </ul>
 * 
 * <p>执行流程：</p>
 * <ol>
 *   <li>从消息内容中提取买家名称</li>
 *   <li>创建发货记录（state=0，待发货）</li>
 *   <li>检查商品是否开启自动发货</li>
 *   <li>获取自动发货配置内容</li>
 *   <li>模拟人工操作延迟</li>
 *   <li>发送发货消息给买家</li>
 *   <li>更新发货记录状态（1=成功，-1=失败）</li>
 * </ol>
 * 
 * @author feijimiao
 * @since 1.0
 */
@Slf4j
@Component
public class ChatMessageEventAutoDeliveryListener {
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;
    
    @Autowired
    private XianyuGoodsOrderMapper orderMapper;
    
    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.SentMessageSaveService sentMessageSaveService;

    @Autowired
    private KamiConfigService kamiConfigService;

    @Autowired
    private XianyuKamiUsageRecordMapper kamiUsageRecordMapper;
    
    /**
     * 处理聊天消息接收事件 - 判断并执行自动发货
     * 
     * @param event 聊天消息接收事件
     */
    @Async
    @EventListener
    public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
        ChatMessageData message = event.getMessageData();
        
        log.info("【账号{}】[AutoDeliveryListener]收到ChatMessageReceivedEvent事件: pnmId={}, contentType={}, msgContent={}, xyGoodsId={}, sId={}, orderId={}", 
                message.getXianyuAccountId(), message.getPnmId(), message.getContentType(), 
                message.getMsgContent(), message.getXyGoodsId(), message.getSId(), message.getOrderId());
        
        try {
            // 判断是否需要触发自动发货
            // 条件1：contentType = 26（已付款待发货）
            // 条件2：msgContent 包含 "[已付款，待发货]"
            if (message.getContentType() == null || message.getContentType() != 26) {
                log.info("【账号{}】[AutoDeliveryListener]contentType不符合条件: contentType={}, 需要26", 
                        message.getXianyuAccountId(), message.getContentType());
                return; // 不是已付款待发货消息
            }
            
            if (message.getMsgContent() == null || 
                (!message.getMsgContent().contains("[已付款，待发货]") && 
                 !message.getMsgContent().contains("[我已付款，等待你发货]"))) {
                log.info("【账号{}】[AutoDeliveryListener]msgContent不符合条件: msgContent={}", 
                        message.getXianyuAccountId(), message.getMsgContent());
                return; // 消息内容不符合条件
            }
            
            log.info("【账号{}】检测到已付款待发货消息: xyGoodsId={}, sId={}, content={}", 
                    message.getXianyuAccountId(), message.getXyGoodsId(), 
                    message.getSId(), message.getMsgContent());
            
            // 检查是否有商品ID和会话ID
            if (message.getXyGoodsId() == null || message.getSId() == null) {
                log.warn("【账号{}】消息缺少商品ID或会话ID，无法触发自动发货: pnmId={}", 
                        message.getXianyuAccountId(), message.getPnmId());
                return;
            }
            
            // 获取买家信息
            String buyerUserName = message.getSenderUserName();
            
            log.info("【账号{}】提取买家信息: buyerUserId={}, buyerUserName={}", 
                    message.getXianyuAccountId(), message.getSenderUserId(), buyerUserName);
            
            // 根据xy_goods_id查询xianyu_goods表获取表ID
            QueryWrapper<XianyuGoodsInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("xy_good_id", message.getXyGoodsId());
            queryWrapper.eq("xianyu_account_id", message.getXianyuAccountId());
            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(queryWrapper);
            
            if (goodsInfo == null) {
                log.warn("【账号{}】未找到商品信息: xyGoodsId={}", 
                        message.getXianyuAccountId(), message.getXyGoodsId());
                return;
            }
            
            log.info("【账号{}】查询到商品信息: xianyuGoodsId={}, title={}", 
                    message.getXianyuAccountId(), goodsInfo.getId(), goodsInfo.getTitle());
            
            // 创建订单记录
            XianyuGoodsOrder record = new XianyuGoodsOrder();
            record.setXianyuAccountId(message.getXianyuAccountId());
            record.setXianyuGoodsId(goodsInfo.getId());
            record.setXyGoodsId(message.getXyGoodsId());
            record.setPnmId(message.getPnmId());
            record.setBuyerUserId(message.getSenderUserId());
            record.setBuyerUserName(buyerUserName);
            record.setOrderId(message.getOrderId());
            record.setContent(null);
            record.setState(0);
            record.setConfirmState(0);
            
            log.info("【账号{}】准备创建订单记录: pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}", 
                    message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId(), 
                    buyerUserName, message.getOrderId());
            
            int result;
            try {
                result = orderMapper.insert(record);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    log.info("【账号{}】消息已处理过，跳过: pnmId={}, xyGoodsId={}", 
                            message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId());
                    return;
                }
                throw e;
            }
            
            if (result > 0) {
                log.info("【账号{}】✅ 创建订单记录成功: recordId={}, pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}", 
                        message.getXianyuAccountId(), record.getId(), message.getPnmId(),
                        message.getXyGoodsId(), buyerUserName, message.getOrderId());
                
                executeAutoDelivery(record.getId(), message.getXianyuAccountId(), 
                        message.getXyGoodsId(), message.getSId(), message.getOrderId(), buyerUserName);
            } else {
                log.error("【账号{}】❌ 创建订单记录失败: pnmId={}, xyGoodsId={}, orderId={}", 
                        message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId(), 
                        message.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("【账号{}】处理自动发货异常: pnmId={}, error={}", 
                    message.getXianyuAccountId(), message.getPnmId(), e.getMessage(), e);
        }
    }
    
    /**
     * 执行自动发货
     * 
     * @param recordId 发货记录ID
     * @param accountId 账号ID
     * @param xyGoodsId 商品ID
     * @param sId 会话ID
     * @param orderId 订单ID
     * @param buyerUserName 买家用户名
     */
    private void executeAutoDelivery(Long recordId, Long accountId, String xyGoodsId, String sId, String orderId, String buyerUserName) {
        try {
            log.info("【账号{}】开始执行自动发货: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId);
            
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            XianyuGoodsAutoDeliveryConfig deliveryConfig = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
            if (deliveryConfig == null) {
                log.warn("【账号{}】商品未配置发货模式: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, -1, null);
                return;
            }

            int deliveryMode = deliveryConfig.getDeliveryMode() != null ? deliveryConfig.getDeliveryMode() : 1;
            String content = null;

            if (deliveryMode == 1) {
                if (deliveryConfig.getAutoDeliveryContent() == null || deliveryConfig.getAutoDeliveryContent().isEmpty()) {
                    log.warn("【账号{}】自动发货模式下未配置发货内容: xyGoodsId={}", accountId, xyGoodsId);
                    updateRecordState(recordId, -1, null);
                    return;
                }
                content = deliveryConfig.getAutoDeliveryContent();
                log.info("【账号{}】自动发货模式: content={}", accountId, content);
            } else if (deliveryMode == 2) {
                content = acquireKamiContent(deliveryConfig.getKamiConfigIds(), deliveryConfig.getKamiDeliveryTemplate(), orderId, accountId, xyGoodsId, sId, recordId, buyerUserName);
                if (content == null) {
                    log.warn("【账号{}】卡密发货模式下无可用卡密: xyGoodsId={}, kamiConfigIds={}", accountId, xyGoodsId, deliveryConfig.getKamiConfigIds());
                    updateRecordState(recordId, -1, "卡密库存不足，无可用卡密");
                    return;
                }
                log.info("【账号{}】卡密发货模式: acquiredKami length={}", accountId, content.length());
            } else if (deliveryMode == 3) {
                log.info("【账号{}】自定义发货模式，不自动发送消息: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, 1, "自定义发货-请通过API处理");
                if (deliveryConfig.getAutoConfirmShipment() != null && deliveryConfig.getAutoConfirmShipment() == 1) {
                    executeAutoConfirmShipment(accountId, orderId);
                }
                return;
            } else {
                log.warn("【账号{}】未知的发货模式: deliveryMode={}", accountId, deliveryMode);
                updateRecordState(recordId, -1, null);
                return;
            }
            
            log.info("【账号{}】准备发送发货消息: deliveryMode={}", accountId, deliveryMode);
            
            HumanLikeDelayUtils.mediumDelay();
            HumanLikeDelayUtils.thinkingDelay();
            HumanLikeDelayUtils.typingDelay(content.length());
            
            String cid = sId.replace("@goofish", "");
            String toId = cid;
            
            String imageUrlStr = deliveryConfig.getAutoDeliveryImageUrl();
            if (imageUrlStr != null && !imageUrlStr.trim().isEmpty()) {
                String[] imageUrls = imageUrlStr.split(",");
                for (int i = 0; i < imageUrls.length; i++) {
                    String url = imageUrls[i].trim();
                    if (!url.isEmpty()) {
                        HumanLikeDelayUtils.thinkingDelay();
                        boolean imgSuccess = webSocketService.sendImageMessage(accountId, cid, toId, url, 800, 800);
                        if (imgSuccess) {
                            log.info("【账号{}】自动发货图片[{}/{}]发送成功: xyGoodsId={}", accountId, i + 1, imageUrls.length, xyGoodsId);
                            sentMessageSaveService.saveManualImageReply(accountId, cid, toId, url, xyGoodsId);
                        } else {
                            log.warn("【账号{}】自动发货图片[{}/{}]发送失败: xyGoodsId={}", accountId, i + 1, imageUrls.length, xyGoodsId);
                        }
                    }
                }
            }
            
            log.info("【账号{}】准备发送发货文本: content长度={}, deliveryMode={}", accountId, content != null ? content.length() : 0, deliveryMode);
            
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);
            
            if (success) {
                log.info("【账号{}】✅ 自动发货成功: recordId={}, xyGoodsId={}, deliveryMode={}", 
                        accountId, recordId, xyGoodsId, deliveryMode);
                updateRecordState(recordId, 1, content);
                
                sentMessageSaveService.saveAiAssistantReply(accountId, cid, toId, content, xyGoodsId);
                
                if (deliveryConfig.getAutoConfirmShipment() != null && deliveryConfig.getAutoConfirmShipment() == 1) {
                    log.info("【账号{}】🚀 检测到自动确认发货开关已开启，准备自动确认发货: orderId={}", accountId, orderId);
                    executeAutoConfirmShipment(accountId, orderId);
                } else {
                    log.info("【账号{}】自动确认发货开关未开启，跳过自动确认发货", accountId);
                }
            } else {
                log.error("【账号{}】❌ 自动发货失败: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId);
                updateRecordState(recordId, -1, content);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】执行自动发货异常: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId, e);
            updateRecordState(recordId, -1, null);
        }
    }

    private String acquireKamiContent(String kamiConfigIds, String kamiDeliveryTemplate, String orderId, Long accountId, String xyGoodsId, String sId, Long recordId, String buyerUserName) {
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
                    log.info("【账号{}】卡密发货成功: configId={}, itemId={}, orderId={}", accountId, configId, kamiItem.getId(), orderId);
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
    
    /**
     * 执行自动确认发货
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     */
    private void executeAutoConfirmShipment(Long accountId, String orderId) {
        try {
            if (orderId == null || orderId.isEmpty()) {
                log.warn("【账号{}】⚠️ 订单ID为空，无法自动确认发货", accountId);
                return;
            }
            
            log.info("【账号{}】开始自动确认发货: orderId={}", accountId, orderId);

            // 模拟人工操作延迟（等待一段时间再确认发货）
            HumanLikeDelayUtils.longDelay(); // 较长延迟，模拟真实操作（2-5秒）

            // 调用确认发货服务
            String result = orderService.confirmShipment(accountId, orderId);
            
            if (result != null) {
                log.info("【账号{}】✅ 自动确认发货成功: orderId={}, result={}", accountId, orderId, result);
            } else {
                log.error("【账号{}】❌ 自动确认发货失败: orderId={}", accountId, orderId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】自动确认发货异常: orderId={}", accountId, orderId, e);
        }
    }
    
    /**
     * 更新发货记录状态和内容
     * 
     * @param recordId 发货记录ID
     * @param state 状态（0=待发货，1=成功，-1=失败）
     * @param content 发货内容
     */
    private void updateRecordState(Long recordId, Integer state, String content) {
        try {
            orderMapper.updateStateAndContent(recordId, state, content);
            log.info("更新订单状态和内容: recordId={}, state={}, content={}", recordId, state, content);
        } catch (Exception e) {
            log.error("更新订单状态和内容失败: recordId={}, state={}, content={}", recordId, state, content, e);
        }
    }
}
