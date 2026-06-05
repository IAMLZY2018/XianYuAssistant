package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;

import java.util.List;

public interface KeywordReplyService {

    List<KeywordReplyRuleBO> getRules(Long accountId, String xyGoodsId);

    KeywordReplyRuleBO addRule(Long accountId, String xyGoodsId, String keyword);

    void deleteRule(Long ruleId);

    void updateKeyword(Long ruleId, String keyword);

    void updateMatchMode(Long ruleId, Integer matchMode);

    KeywordReplyRuleBO ensureFallbackRule(Long accountId, String xyGoodsId);

    KeywordReplyRuleBO.KeywordReplyContentBO addContent(Long ruleId, String replyText, String replyImageUrl);

    void updateContent(Long contentId, String replyText, String replyImageUrl);

    void deleteContent(Long contentId);

    List<KeywordReplyRuleBO> matchKeyword(Long accountId, String xyGoodsId, String message);

    boolean isKeywordReplyEnabled(Long accountId, String xyGoodsId);
}
