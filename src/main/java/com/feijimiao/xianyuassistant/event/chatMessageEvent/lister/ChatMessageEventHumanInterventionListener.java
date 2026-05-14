package com.feijimiao.xianyuassistant.event.chatMessageEvent.lister;

import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageReceivedEvent;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.AutoReplyDelayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 人工干预事件监听器
 *
 * <p>监听 {@link ChatMessageReceivedEvent} 事件，检测卖家通过闲鱼APP手动回复的消息，
 * 触发人工接管，在指定时间内暂停该会话的AI自动回复。</p>
 *
 * <p>触发条件：</p>
 * <ul>
 *   <li>contentType = 1（用户消息）</li>
 *   <li>消息发送者是自己（卖家通过闲鱼APP直接回复）</li>
 *   <li>商品开启了人工干预开关</li>
 * </ul>
 *
 * <p>与 {@link ChatMessageEventAutoReplyListener} 的区别：</p>
 * <ul>
 *   <li>AutoReplyListener：买家消息 → 触发自动回复</li>
 *   <li>本Listener：卖家消息 → 触发人工接管，暂停自动回复</li>
 * </ul>
 *
 * @author IAMLZY
 * @date 2026/5/14
 */
@Slf4j
@Component
public class ChatMessageEventHumanInterventionListener {

    @Autowired
    private AutoReplyDelayService autoReplyDelayService;

    @Autowired
    private AccountService accountService;

    /**
     * 处理聊天消息接收事件 - 检测卖家手动回复并触发人工接管
     *
     * @param event 聊天消息接收事件
     */
    @Async
    @EventListener
    public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
        ChatMessageData message = event.getMessageData();

        try {
            // 1. 只处理用户消息（contentType=1）
            if (message.getContentType() == null || message.getContentType() != 1) {
                return;
            }

            // 2. 判断是否为自己发送的消息（卖家手动回复）
            String senderUserId = message.getSenderUserId();
            String ownUserId = accountService.getXianyuUserId(message.getXianyuAccountId());

            if (senderUserId == null || ownUserId == null || !senderUserId.equals(ownUserId)) {
                return;
            }

            // 3. 检查是否有商品ID和会话ID
            if (message.getXyGoodsId() == null || message.getSId() == null) {
                log.debug("【账号{}】卖家手动回复缺少商品ID或会话ID，跳过人工接管: pnmId={}",
                        message.getXianyuAccountId(), message.getPnmId());
                return;
            }

            log.info("【账号{}】[HumanInterventionListener]检测到卖家手动回复，触发人工接管: xyGoodsId={}, sId={}, content={}",
                    message.getXianyuAccountId(), message.getXyGoodsId(), message.getSId(), message.getMsgContent());

            // 4. 触发人工接管（内部会检查人工干预开关、标记接管、取消待执行延时任务）
            autoReplyDelayService.recordSellerManualReply(
                    message.getXianyuAccountId(),
                    message.getXyGoodsId(),
                    message.getSId()
            );

        } catch (Exception e) {
            log.error("【账号{}】处理人工干预事件异常: pnmId={}",
                    message.getXianyuAccountId(), message.getPnmId(), e);
        }
    }
}
