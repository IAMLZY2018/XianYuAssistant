package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuKeywordReplyContent;
import com.feijimiao.xianyuassistant.entity.XianyuKeywordReplyRule;
import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKeywordReplyContentMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKeywordReplyRuleMapper;
import com.feijimiao.xianyuassistant.service.KeywordReplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeywordReplyServiceImpl implements KeywordReplyService {

    @Autowired
    private XianyuKeywordReplyRuleMapper ruleMapper;

    @Autowired
    private XianyuKeywordReplyContentMapper contentMapper;

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Override
    public List<KeywordReplyRuleBO> getRules(Long accountId, String xyGoodsId) {
        List<XianyuKeywordReplyRule> rules = ruleMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ruleIds = rules.stream().map(XianyuKeywordReplyRule::getId).collect(Collectors.toList());
        List<XianyuKeywordReplyContent> allContents = new ArrayList<>();
        for (Long ruleId : ruleIds) {
            List<XianyuKeywordReplyContent> contents = contentMapper.selectByRuleId(ruleId);
            if (contents != null) {
                allContents.addAll(contents);
            }
        }
        Map<Long, List<XianyuKeywordReplyContent>> contentMap = allContents.stream()
                .collect(Collectors.groupingBy(XianyuKeywordReplyContent::getRuleId));

        return rules.stream().map(rule -> toRuleBO(rule, contentMap.getOrDefault(rule.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public KeywordReplyRuleBO addRule(Long accountId, String xyGoodsId, String keyword) {
        XianyuKeywordReplyRule existing = ruleMapper.selectByKeyword(accountId, xyGoodsId, keyword);
        if (existing != null) {
            return toRuleBO(existing, Collections.emptyList());
        }

        XianyuKeywordReplyRule rule = new XianyuKeywordReplyRule();
        rule.setXianyuAccountId(accountId);
        rule.setXyGoodsId(xyGoodsId);
        rule.setKeyword(keyword);
        rule.setMatchMode(1);
        rule.setIsFallback(0);
        ruleMapper.insert(rule);

        KeywordReplyRuleBO bo = new KeywordReplyRuleBO();
        bo.setId(rule.getId());
        bo.setXianyuAccountId(accountId);
        bo.setXyGoodsId(xyGoodsId);
        bo.setKeyword(keyword);
        bo.setMatchMode(1);
        bo.setIsFallback(0);
        bo.setContents(Collections.emptyList());
        return bo;
    }

    @Override
    public void deleteRule(Long ruleId) {
        contentMapper.deleteByRuleId(ruleId);
        ruleMapper.deleteById(ruleId);
    }

    @Override
    public void updateKeyword(Long ruleId, String keyword) {
        XianyuKeywordReplyRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("规则不存在: id=" + ruleId);
        }
        rule.setKeyword(keyword);
        ruleMapper.updateById(rule);
    }

    @Override
    public void updateMatchMode(Long ruleId, Integer matchMode) {
        XianyuKeywordReplyRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new RuntimeException("规则不存在: id=" + ruleId);
        }
        rule.setMatchMode(matchMode);
        ruleMapper.updateById(rule);
    }

    @Override
    public KeywordReplyRuleBO ensureFallbackRule(Long accountId, String xyGoodsId) {
        XianyuKeywordReplyRule existing = ruleMapper.selectFallback(accountId, xyGoodsId);
        if (existing != null) {
            List<XianyuKeywordReplyContent> contents = contentMapper.selectByRuleId(existing.getId());
            return toRuleBO(existing, contents != null ? contents : Collections.emptyList());
        }

        XianyuKeywordReplyRule rule = new XianyuKeywordReplyRule();
        rule.setXianyuAccountId(accountId);
        rule.setXyGoodsId(xyGoodsId);
        rule.setKeyword("__fallback__");
        rule.setMatchMode(1);
        rule.setIsFallback(1);
        ruleMapper.insert(rule);

        KeywordReplyRuleBO bo = new KeywordReplyRuleBO();
        bo.setId(rule.getId());
        bo.setXianyuAccountId(accountId);
        bo.setXyGoodsId(xyGoodsId);
        bo.setKeyword("__fallback__");
        bo.setMatchMode(1);
        bo.setIsFallback(1);
        bo.setContents(Collections.emptyList());
        return bo;
    }

    @Override
    public KeywordReplyRuleBO.KeywordReplyContentBO addContent(Long ruleId, String replyText, String replyImageUrl) {
        XianyuKeywordReplyContent content = new XianyuKeywordReplyContent();
        content.setRuleId(ruleId);
        content.setReplyText(replyText);
        content.setReplyImageUrl(replyImageUrl);
        contentMapper.insert(content);

        KeywordReplyRuleBO.KeywordReplyContentBO bo = new KeywordReplyRuleBO.KeywordReplyContentBO();
        bo.setId(content.getId());
        bo.setRuleId(ruleId);
        bo.setReplyText(replyText);
        bo.setReplyImageUrl(replyImageUrl);
        return bo;
    }

    @Override
    public void updateContent(Long contentId, String replyText, String replyImageUrl) {
        XianyuKeywordReplyContent content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new RuntimeException("回复内容不存在: id=" + contentId);
        }
        content.setReplyText(replyText);
        content.setReplyImageUrl(replyImageUrl);
        contentMapper.updateById(content);
    }

    @Override
    public void deleteContent(Long contentId) {
        contentMapper.deleteById(contentId);
    }

    @Override
    public List<KeywordReplyRuleBO> matchKeyword(Long accountId, String xyGoodsId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String msg = message.trim();

        List<XianyuKeywordReplyRule> exactMatches = ruleMapper.matchExact(accountId, xyGoodsId, msg);
        List<XianyuKeywordReplyRule> fuzzyMatches = ruleMapper.matchFuzzy(accountId, xyGoodsId, msg);

        Set<Long> matchedIds = new HashSet<>();
        List<XianyuKeywordReplyRule> allMatches = new ArrayList<>();
        for (XianyuKeywordReplyRule r : exactMatches) {
            if (matchedIds.add(r.getId())) {
                allMatches.add(r);
            }
        }
        for (XianyuKeywordReplyRule r : fuzzyMatches) {
            if (matchedIds.add(r.getId())) {
                allMatches.add(r);
            }
        }

        if (!allMatches.isEmpty()) {
            return allMatches.stream().map(rule -> {
                List<XianyuKeywordReplyContent> contents = contentMapper.selectByRuleId(rule.getId());
                return toRuleBO(rule, contents != null ? contents : Collections.emptyList());
            }).collect(Collectors.toList());
        }

        XianyuKeywordReplyRule fallback = ruleMapper.selectFallback(accountId, xyGoodsId);
        if (fallback != null) {
            List<XianyuKeywordReplyContent> contents = contentMapper.selectByRuleId(fallback.getId());
            if (contents != null && !contents.isEmpty()) {
                return Collections.singletonList(toRuleBO(fallback, contents));
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isKeywordReplyEnabled(Long accountId, String xyGoodsId) {
        if (accountId == null || xyGoodsId == null) {
            return false;
        }
        try {
            XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            return config != null && config.getXianyuKeywordReplyOn() != null && config.getXianyuKeywordReplyOn() == 1;
        } catch (Exception e) {
            log.error("检查关键词回复开关异常: accountId={}, xyGoodsId={}", accountId, xyGoodsId, e);
            return false;
        }
    }

    private KeywordReplyRuleBO toRuleBO(XianyuKeywordReplyRule rule, List<XianyuKeywordReplyContent> contents) {
        KeywordReplyRuleBO bo = new KeywordReplyRuleBO();
        bo.setId(rule.getId());
        bo.setXianyuAccountId(rule.getXianyuAccountId());
        bo.setXyGoodsId(rule.getXyGoodsId());
        bo.setKeyword(rule.getKeyword());
        bo.setMatchMode(rule.getMatchMode());
        bo.setIsFallback(rule.getIsFallback());
        bo.setContents(contents.stream().map(c -> {
            KeywordReplyRuleBO.KeywordReplyContentBO cbo = new KeywordReplyRuleBO.KeywordReplyContentBO();
            cbo.setId(c.getId());
            cbo.setRuleId(c.getRuleId());
            cbo.setReplyText(c.getReplyText());
            cbo.setReplyImageUrl(c.getReplyImageUrl());
            return cbo;
        }).collect(Collectors.toList()));
        return bo;
    }
}
