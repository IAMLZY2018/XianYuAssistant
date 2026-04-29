package com.feijimiao.xianyuassistant.event.chatMessageEvent.lister;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageReceivedEvent;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 自动发货事件监听器
 *
 * <p>监听 {@link ChatMessageReceivedEvent} 事件，判断是否需要触发自动发货</p>
 *
 * <p>触发条件：</p>
 * <ul>
 *   <li>contentType = 26（已付款待发货类型）</li>
 *   <li>msgContent 包含 "[已付款，待发货]" 或 "[我已付款，等待你发货]"</li>
 * </ul>
 *
 * <p>职责：事件过滤 + 订单记录创建，发货执行委派给 {@link AutoDeliveryService#executeDelivery}</p>
 */
@Slf4j
@Component
public class ChatMessageEventAutoDeliveryListener {

    @Autowired
    private XianyuGoodsOrderMapper orderMapper;

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;

    @Autowired
    private AutoDeliveryService autoDeliveryService;

    @Async
    @EventListener
    public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
        ChatMessageData message = event.getMessageData();
        Long accountId = message.getXianyuAccountId();

        log.info("【账号{}】[AutoDeliveryListener]收到事件: pnmId={}, contentType={}, xyGoodsId={}, sId={}, orderId={}",
                accountId, message.getPnmId(), message.getContentType(),
                message.getXyGoodsId(), message.getSId(), message.getOrderId());

        try {
            if (!isPaymentMessage(message)) {
                return;
            }

            if (message.getXyGoodsId() == null || message.getSId() == null) {
                log.warn("【账号{}】消息缺少商品ID或会话ID，无法触发自动发货: pnmId={}", accountId, message.getPnmId());
                return;
            }

            String buyerUserName = message.getSenderUserName();
            log.info("【账号{}】检测到已付款待发货消息: xyGoodsId={}, buyerUserId={}, orderId={}",
                    accountId, message.getXyGoodsId(), message.getSenderUserId(), message.getOrderId());

            Long xianyuGoodsId = resolveXianyuGoodsId(accountId, message.getXyGoodsId());
            if (xianyuGoodsId == null) {
                return;
            }

            Long recordId = createOrderRecord(accountId, xianyuGoodsId, message);
            if (recordId == null) {
                return;
            }

            autoDeliveryService.executeDelivery(
                    recordId, accountId, message.getXyGoodsId(), message.getSId(),
                    message.getOrderId(), buyerUserName, true);

        } catch (Exception e) {
            log.error("【账号{}】处理自动发货异常: pnmId={}", accountId, message.getPnmId(), e);
        }
    }

    private boolean isPaymentMessage(ChatMessageData message) {
        if (message.getContentType() == null || message.getContentType() != 26) {
            return false;
        }
        if (message.getMsgContent() == null) {
            return false;
        }
        return message.getMsgContent().contains("[已付款，待发货]")
                || message.getMsgContent().contains("[我已付款，等待你发货]");
    }

    private Long resolveXianyuGoodsId(Long accountId, String xyGoodsId) {
        QueryWrapper<XianyuGoodsInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("xy_good_id", xyGoodsId);
        queryWrapper.eq("xianyu_account_id", accountId);
        XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(queryWrapper);
        if (goodsInfo == null) {
            log.warn("【账号{}】未找到商品信息: xyGoodsId={}", accountId, xyGoodsId);
            return null;
        }
        return goodsInfo.getId();
    }

    private Long createOrderRecord(Long accountId, Long xianyuGoodsId, ChatMessageData message) {
        XianyuGoodsOrder record = new XianyuGoodsOrder();
        record.setXianyuAccountId(accountId);
        record.setXianyuGoodsId(xianyuGoodsId);
        record.setXyGoodsId(message.getXyGoodsId());
        record.setPnmId(message.getPnmId());
        record.setBuyerUserId(message.getSenderUserId());
        record.setBuyerUserName(message.getSenderUserName());
        record.setSid(message.getSId());
        record.setOrderId(message.getOrderId());
        record.setContent(null);
        record.setState(0);
        record.setConfirmState(0);

        try {
            int result = orderMapper.insert(record);
            if (result > 0) {
                log.info("【账号{}】✅ 创建订单记录成功: recordId={}, orderId={}", accountId, record.getId(), message.getOrderId());
                return record.getId();
            } else {
                log.error("【账号{}】❌ 创建订单记录失败: pnmId={}, orderId={}", accountId, message.getPnmId(), message.getOrderId());
                return null;
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                log.info("【账号{}】消息已处理过，跳过: pnmId={}, xyGoodsId={}", accountId, message.getPnmId(), message.getXyGoodsId());
                return null;
            }
            throw new RuntimeException("创建订单记录失败", e);
        }
    }
}
