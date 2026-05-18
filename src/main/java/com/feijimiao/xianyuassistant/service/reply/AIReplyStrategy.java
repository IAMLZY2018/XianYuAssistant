package com.feijimiao.xianyuassistant.service.reply;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AIReplyStrategy implements ReplyStrategy {

    private static final int REPLY_TYPE_AI = 2;

    @Autowired
    private AIService aiService;

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;

    @Override
    public ReplyResult execute(List<ChatMessageData> messageList) {
        ChatMessageData lastMessage = messageList.get(messageList.size() - 1);
        Long accountId = lastMessage.getXianyuAccountId();
        String xyGoodsId = lastMessage.getXyGoodsId();

        String buyerMessage = messageList.stream()
                .map(ChatMessageData::getMsgContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        try {
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            String fixedMaterial = goodsConfig != null ? goodsConfig.getFixedMaterial() : null;

            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(
                    new LambdaQueryWrapper<XianyuGoodsInfo>().eq(XianyuGoodsInfo::getXyGoodId, xyGoodsId)
            );
            String goodsDetail = goodsInfo != null ? goodsInfo.getDetailInfo() : null;

            RAGReplyResult result = aiService.chatByRAGWithFixedMaterial(buyerMessage, xyGoodsId, fixedMaterial, goodsDetail);

            if (result != null && result.getReplyContent() != null && !result.getReplyContent().trim().isEmpty()) {
                return ReplyResult.of(Collections.singletonList(
                        ReplyResult.ReplyItem.text(result.getReplyContent(), REPLY_TYPE_AI)
                ));
            }
            return ReplyResult.fail();
        } catch (Exception e) {
            log.error("【账号{}】AI回复策略执行失败: xyGoodsId={}", accountId, xyGoodsId, e);
            return ReplyResult.fail();
        }
    }
}
