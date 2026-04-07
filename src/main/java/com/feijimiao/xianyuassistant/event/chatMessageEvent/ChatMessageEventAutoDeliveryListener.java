package com.feijimiao.xianyuassistant.event.chatMessageEvent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 聊天消息自动发货监听器
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
    private XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;
    
    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private OrderService orderService;
    
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
            
            if (message.getMsgContent() == null || !message.getMsgContent().contains("[已付款，待发货]")) {
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
            
            // 创建发货记录（state=0，待发货）
            XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
            record.setXianyuAccountId(message.getXianyuAccountId());
            record.setXianyuGoodsId(goodsInfo.getId()); // 设置本地商品表ID
            record.setXyGoodsId(message.getXyGoodsId()); // 设置闲鱼商品ID
            record.setPnmId(message.getPnmId()); // 设置消息pnmId，用于防重复
            record.setBuyerUserId(message.getSenderUserId());
            record.setBuyerUserName(buyerUserName);
            record.setOrderId(message.getOrderId()); // 设置订单ID
            record.setContent(null); // 内容稍后设置
            record.setState(0); // 0=待发货
            
            log.info("【账号{}】准备创建发货记录: pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}", 
                    message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId(), 
                    buyerUserName, message.getOrderId());
            
            int result;
            try {
                result = autoDeliveryRecordMapper.insert(record);
            } catch (Exception e) {
                // 检查是否是唯一约束冲突（pnm_id重复）
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    log.info("【账号{}】消息已处理过，跳过自动发货: pnmId={}, xyGoodsId={}", 
                            message.getXianyuAccountId(), message.getPnmId(), message.getXyGoodsId());
                    return; // 消息已处理，直接返回
                }
                throw e; // 其他异常继续抛出
            }
            
            if (result > 0) {
                log.info("【账号{}】✅ 创建发货记录成功: recordId={}, pnmId={}, xyGoodsId={}, buyerUserName={}, orderId={}, state=0（待发货）", 
                        message.getXianyuAccountId(), record.getId(), message.getPnmId(),
                        message.getXyGoodsId(), buyerUserName, message.getOrderId());
                
                // 执行自动发货
                executeAutoDelivery(record.getId(), message.getXianyuAccountId(), 
                        message.getXyGoodsId(), message.getSId(), message.getOrderId());
            } else {
                log.error("【账号{}】❌ 创建发货记录失败: pnmId={}, xyGoodsId={}, orderId={}", 
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
     */
    private void executeAutoDelivery(Long recordId, Long accountId, String xyGoodsId, String sId, String orderId) {
        try {
            log.info("【账号{}】开始执行自动发货: recordId={}, xyGoodsId={}", accountId, recordId, xyGoodsId);
            
            // 1. 检查商品是否开启自动发货
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, -1, null);
                return;
            }
            
            // 2. 获取自动发货配置
            XianyuGoodsAutoDeliveryConfig deliveryConfig = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
            if (deliveryConfig == null || deliveryConfig.getAutoDeliveryContent() == null || 
                    deliveryConfig.getAutoDeliveryContent().isEmpty()) {
                log.warn("【账号{}】商品未配置自动发货内容: xyGoodsId={}", accountId, xyGoodsId);
                updateRecordState(recordId, -1, null);
                return;
            }
            
            String content = deliveryConfig.getAutoDeliveryContent();
            log.info("【账号{}】准备发送自动发货消息: content={}", accountId, content);
            
            // 3. 模拟人工操作：阅读消息 + 思考 + 打字延迟
            log.info("【账号{}】模拟人工操作延迟...", accountId);
            HumanLikeDelayUtils.mediumDelay();      // 阅读延迟
            HumanLikeDelayUtils.thinkingDelay();    // 思考延迟
            HumanLikeDelayUtils.typingDelay(content.length()); // 打字延迟
            
            // 4. 从sId中提取cid和toId
            String cid = sId.replace("@goofish", "");
            String toId = cid;
            
            // 5. 发送消息
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);
            
            // 6. 更新发货记录状态和内容
            if (success) {
                log.info("【账号{}】✅ 自动发货成功: recordId={}, xyGoodsId={}, content={}", 
                        accountId, recordId, xyGoodsId, content);
                updateRecordState(recordId, 1, content);
                
                // 7. 检查是否需要自动确认发货
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
            log.info("【账号{}】模拟人工操作延迟（等待后确认发货）...", accountId);
            HumanLikeDelayUtils.longDelay(); // 较长延迟，模拟真实操作
            
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
            autoDeliveryRecordMapper.updateStateAndContent(recordId, state, content);
            log.info("更新发货记录状态和内容: recordId={}, state={}, content={}", recordId, state, content);
        } catch (Exception e) {
            log.error("更新发货记录状态和内容失败: recordId={}, state={}, content={}", recordId, state, content, e);
        }
    }
}
