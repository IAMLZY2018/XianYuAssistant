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
 * 自动回复事件监听器
 * 
 * <p>监听 {@link ChatMessageReceivedEvent} 事件，判断是否需要触发RAG自动回复</p>
 * 
 * <p>触发条件：</p>
 * <ul>
 *   <li>contentType = 1（用户消息）</li>
 *   <li>消息发送者不是自己（比对闲鱼用户ID）</li>
 *   <li>商品开启了自动回复</li>
 * </ul>
 * 
 * <p>执行流程：</p>
 * <ol>
 *   <li>判断消息类型是否为用户消息（contentType=1）</li>
 *   <li>判断消息发送者是否为自己（比对senderUserId和账号的闲鱼用户ID）</li>
 *   <li>检查商品是否开启自动回复</li>
 *   <li>提交延时任务（N秒后执行）</li>
 *   <li>如果N秒内有新消息，取消旧任务，重新计时</li>
 * </ol>
 * 
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@Component
public class ChatMessageEventAutoReplyListener {
    
    @Autowired
    private AutoReplyDelayService autoReplyDelayService;
    
    @Autowired
    private AccountService accountService;
    
    /**
     * 处理聊天消息接收事件 - 判断并触发RAG自动回复
     * 
     * @param event 聊天消息接收事件
     */
    @Async
    @EventListener
    public void handleChatMessageReceived(ChatMessageReceivedEvent event) {
        ChatMessageData message = event.getMessageData();
        
        log.info("【账号{}】[AutoReplyListener]收到ChatMessageReceivedEvent事件: pnmId={}, contentType={}, senderUserId={}, msgContent={}, xyGoodsId={}, sId={}", 
                message.getXianyuAccountId(), message.getPnmId(), message.getContentType(),
                message.getSenderUserId(), message.getMsgContent(), message.getXyGoodsId(), message.getSId());
        
        try {
            // 1. 判断是否为用户消息（contentType=1）
            if (message.getContentType() == null || message.getContentType() != 1) {
                log.debug("【账号{}】[AutoReplyListener]非用户消息，跳过: contentType={}", 
                        message.getXianyuAccountId(), message.getContentType());
                return;
            }
            
            // 2. 判断是否为自己发送的消息
            String senderUserId = message.getSenderUserId();
            String ownUserId = accountService.getXianyuUserId(message.getXianyuAccountId());
            
            if (senderUserId != null && ownUserId != null && senderUserId.equals(ownUserId)) {
                log.debug("【账号{}】[AutoReplyListener]自己发送的消息，跳过自动回复: senderUserId={}, ownUserId={}", 
                        message.getXianyuAccountId(), senderUserId, ownUserId);
                return;
            }
            
            log.debug("【账号{}】[AutoReplyListener]消息发送者: senderUserId={}, ownUserId={}", 
                    message.getXianyuAccountId(), senderUserId, ownUserId);
            
            // 3. 检查是否有商品ID和会话ID
            if (message.getXyGoodsId() == null || message.getSId() == null) {
                log.debug("【账号{}】消息缺少商品ID或会话ID，跳过自动回复: pnmId={}", 
                        message.getXianyuAccountId(), message.getPnmId());
                return;
            }
            
            // 4. 检查消息内容是否为空
            if (message.getMsgContent() == null || message.getMsgContent().trim().isEmpty()) {
                log.debug("【账号{}】消息内容为空，跳过自动回复: pnmId={}", 
                        message.getXianyuAccountId(), message.getPnmId());
                return;
            }
            
            log.info("【账号{}】检测到用户消息（非自己发送），提交延时回复任务: xyGoodsId={}, sId={}, content={}", 
                    message.getXianyuAccountId(), message.getXyGoodsId(), 
                    message.getSId(), message.getMsgContent());
            
            // 5. 提交延时任务（N秒后执行RAG自动回复）
            // 如果该会话已有待执行的任务，会先取消旧任务再提交新任务
            autoReplyDelayService.submitDelayTask(message);
            
        } catch (Exception e) {
            log.error("【账号{}】处理自动回复事件异常: pnmId={}", 
                    message.getXianyuAccountId(), message.getPnmId(), e);
        }
    }
}
