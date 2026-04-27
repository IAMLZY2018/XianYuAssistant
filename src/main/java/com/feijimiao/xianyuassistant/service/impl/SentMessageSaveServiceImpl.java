package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.context.UserContext;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.SentMessageSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 发送消息入库服务实现
 * 
 * <p>自己发送或AI自动回复的消息，闲鱼不会推送同步消息，需要主动入库</p>
 */
@Slf4j
@Service
public class SentMessageSaveServiceImpl implements SentMessageSaveService {
    
    @Autowired
    private XianyuChatMessageMapper chatMessageMapper;
    
    @Autowired
    private AccountService accountService;
    
    /** 手动回复 contentType */
    private static final int CONTENT_TYPE_MANUAL_REPLY = 999;
    
    /** AI助手回复 contentType */
    private static final int CONTENT_TYPE_AI_ASSISTANT_REPLY = 888;

    /** 图片回复 contentType */
    private static final int CONTENT_TYPE_IMAGE_REPLY = 997;
    
    @Override
    @Async
    public void saveManualReply(Long accountId, String cid, String toId, String text, String xyGoodsId) {
        // 从UserContext获取当前登录用户名
        String username = UserContext.getUsername();
        saveSentMessage(accountId, cid, toId, text, CONTENT_TYPE_MANUAL_REPLY, 
                username != null ? username : "手动回复", xyGoodsId);
    }
    
    @Override
    @Async
    public void saveAiAssistantReply(Long accountId, String cid, String toId, String text, String xyGoodsId) {
        saveSentMessage(accountId, cid, toId, text, CONTENT_TYPE_AI_ASSISTANT_REPLY, "AI助手", xyGoodsId);
    }

    @Override
    @Async
    public void saveManualImageReply(Long accountId, String cid, String toId, String imageUrl, String xyGoodsId) {
        saveSentMessage(accountId, cid, toId, "[图片]" + imageUrl, CONTENT_TYPE_IMAGE_REPLY, "图片回复", xyGoodsId);
    }
    
    /**
     * 保存发送的消息到数据库
     */
    private void saveSentMessage(Long accountId, String cid, String toId, String text, 
                                 int contentType, String senderUserName, String xyGoodsId) {
        try {
            // 获取当前账号的闲鱼用户ID（作为发送者）
            String ownUserId = accountService.getXianyuUserId(accountId);
            
            // 构建消息实体
            XianyuChatMessage message = new XianyuChatMessage();
            message.setXianyuAccountId(accountId);
            message.setLwp("/r/MessageSend/sendByReceiverScope");
            
            // 生成唯一的pnmId（发送的消息没有闲鱼推送的pnmId，需要自己生成）
            String pnmId = generatePnmId();
            message.setPnmId(pnmId);
            
            // 设置会话ID（确保带@goofish后缀）
            String cleanCid = cid.replace("@goofish", "");
            String sId = cleanCid + "@goofish";
            message.setSId(sId);
            
            message.setContentType(contentType);
            message.setMsgContent(text);
            
            // 发送者信息
            message.setSenderUserId(ownUserId);
            message.setSenderUserName(senderUserName);
            
            // 商品ID
            message.setXyGoodsId(xyGoodsId);
            
            // 消息时间
            message.setMessageTime(System.currentTimeMillis());
            
            // complete_msg字段为NOT NULL，构建一个简单的JSON
            String completeMsg = String.format(
                    "{\"source\":\"%s\",\"contentType\":%d,\"cid\":\"%s\",\"toId\":\"%s\",\"xyGoodsId\":\"%s\",\"text\":\"%s\"}",
                    senderUserName, contentType, cleanCid, toId,
                    xyGoodsId != null ? xyGoodsId : "",
                    text != null ? text.replace("\\", "\\\\").replace("\"", "\\\"") : "");
            message.setCompleteMsg(completeMsg);
            
            // 保存到数据库
            int result = chatMessageMapper.insert(message);
            
            if (result > 0) {
                log.info("【账号{}】[{}]消息入库成功: pnmId={}, cid={}, toId={}, xyGoodsId={}, text={}", 
                        accountId, senderUserName, pnmId, cid, toId, xyGoodsId, 
                        text.length() > 50 ? text.substring(0, 50) + "..." : text);
            } else {
                log.error("【账号{}】[{}]消息入库失败: pnmId={}", accountId, senderUserName, pnmId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】[{}]消息入库异常: cid={}, toId={}", accountId, senderUserName, cid, toId, e);
        }
    }
    
    /**
     * 生成唯一的pnmId
     * 格式: 时间戳 + 随机数 + ".PNM"
     */
    private String generatePnmId() {
        return System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ".PNM";
    }
}
