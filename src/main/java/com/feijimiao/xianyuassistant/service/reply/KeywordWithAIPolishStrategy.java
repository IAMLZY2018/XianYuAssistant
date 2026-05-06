package com.feijimiao.xianyuassistant.service.reply;

import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.KeywordReplyService;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KeywordWithAIPolishStrategy implements ReplyStrategy {

    private static final int REPLY_TYPE_KEYWORD_AI = 3;
    private static final int REPLY_TYPE_AI = 2;

    @Autowired
    private KeywordReplyService keywordReplyService;

    @Autowired
    private AIService aiService;

    @Autowired
    private DynamicAIChatClientManager dynamicAIChatClientManager;

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

        List<KeywordReplyRuleBO> matchedRules = keywordReplyService.matchKeyword(accountId, xyGoodsId, buyerMessage);

        if (matchedRules != null && !matchedRules.isEmpty()) {
            return executeKeywordWithPolish(accountId, matchedRules);
        }

        return executeAIReply(accountId, xyGoodsId, buyerMessage);
    }

    private ReplyResult executeKeywordWithPolish(Long accountId, List<KeywordReplyRuleBO> matchedRules) {
        List<KeywordReplyRuleBO.KeywordReplyContentBO> allContents = matchedRules.stream()
                .filter(r -> r.getContents() != null)
                .flatMap(r -> r.getContents().stream())
                .collect(Collectors.toList());

        if (allContents.isEmpty()) {
            return ReplyResult.fail();
        }

        KeywordReplyRuleBO.KeywordReplyContentBO selected = allContents.get(new Random().nextInt(allContents.size()));
        List<ReplyResult.ReplyItem> items = new ArrayList<>();
        String originalText = selected.getReplyText();
        String image = selected.getReplyImageUrl();
        boolean hasText = originalText != null && !originalText.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();

        String finalText = originalText;
        if (hasText && dynamicAIChatClientManager.isAvailable() && aiService != null) {
            try {
                String polishPrompt = String.format(
                        "你是一个闲鱼卖家，请用自然亲切的语气简单润色以下回复内容，保持原意不变，不要添加额外信息，直接输出润色后的内容：\n\n%s",
                        originalText
                );
                String polishedText = aiService.simpleChat(polishPrompt);
                if (polishedText != null && !polishedText.trim().isEmpty()) {
                    finalText = polishedText;
                }
            } catch (Exception e) {
                log.warn("【账号{}】AI润化失败，使用原文回复: {}", accountId, e.getMessage());
            }
        }

        boolean finalHasText = finalText != null && !finalText.trim().isEmpty();
        if (finalHasText && hasImage) {
            items.add(ReplyResult.ReplyItem.textAndImage(finalText, image, REPLY_TYPE_KEYWORD_AI));
        } else if (finalHasText) {
            items.add(ReplyResult.ReplyItem.text(finalText, REPLY_TYPE_KEYWORD_AI));
        } else if (hasImage) {
            items.add(ReplyResult.ReplyItem.image(image, REPLY_TYPE_KEYWORD_AI));
        }

        if (items.isEmpty()) {
            return ReplyResult.fail();
        }

        ReplyResult result = ReplyResult.of(items);
        String keywords = matchedRules.stream()
                .filter(r -> r.getIsFallback() == null || r.getIsFallback() == 0)
                .map(KeywordReplyRuleBO::getKeyword)
                .collect(Collectors.joining(", "));
        result.setMatchedKeyword(keywords);
        result.setMatchedRules(matchedRules);
        if (!matchedRules.isEmpty()) {
            result.setMatchedRule(matchedRules.get(0));
        }
        return result;
    }

    private ReplyResult executeAIReply(Long accountId, String xyGoodsId, String buyerMessage) {
        try {
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            String fixedMaterial = goodsConfig != null ? goodsConfig.getFixedMaterial() : null;

            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(
                    new LambdaQueryWrapper<XianyuGoodsInfo>().eq(XianyuGoodsInfo::getXyGoodId, xyGoodsId)
            );
            String goodsDetail = goodsInfo != null ? goodsInfo.getDetailInfo() : null;

            RAGReplyResult ragResult = aiService.chatByRAGWithFixedMaterial(buyerMessage, xyGoodsId, fixedMaterial, goodsDetail);

            if (ragResult != null && ragResult.getReplyContent() != null && !ragResult.getReplyContent().trim().isEmpty()) {
                return ReplyResult.of(Collections.singletonList(
                        ReplyResult.ReplyItem.text(ragResult.getReplyContent(), REPLY_TYPE_AI)
                ));
            }
            return ReplyResult.fail();
        } catch (Exception e) {
            log.error("【账号{}】AI回复失败: xyGoodsId={}", accountId, xyGoodsId, e);
            return ReplyResult.fail();
        }
    }
}
