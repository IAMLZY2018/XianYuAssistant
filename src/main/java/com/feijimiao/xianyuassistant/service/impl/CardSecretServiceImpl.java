package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsCardSecretMapper;
import com.feijimiao.xianyuassistant.service.CardSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 卡密管理服务实现
 */
@Slf4j
@Service
public class CardSecretServiceImpl extends ServiceImpl<XianyuGoodsCardSecretMapper, XianyuGoodsCardSecret> implements CardSecretService {

    @Override
    @Transactional
    public void addCardSecret(XianyuGoodsCardSecret cardSecret) {
        if (cardSecret.getXyGoodsId() != null) {
            cardSecret.setXyGoodsId(cardSecret.getXyGoodsId().trim());
        }
        cardSecret.setIsUsed(0);
        this.save(cardSecret);
    }

    @Override
    @Transactional
    public int importCardSecrets(Long accountId, String xyGoodsId, List<String> secrets) {
        String trimmedGoodsId = xyGoodsId != null ? xyGoodsId.trim() : "";
        List<XianyuGoodsCardSecret> entities = secrets.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> {
                    XianyuGoodsCardSecret entity = new XianyuGoodsCardSecret();
                    entity.setXianyuAccountId(accountId);
                    entity.setXyGoodsId(trimmedGoodsId);
                    entity.setCardContent(s.trim());
                    entity.setIsUsed(0);
                    return entity;
                }).collect(Collectors.toList());
        
        if (entities.isEmpty()) return 0;
        this.saveBatch(entities);
        return entities.size();
    }

    @Override
    public IPage<CardSecretRespDTO> selectCardSecretPage(CardSecretQueryReqDTO reqDTO) {
        String goodsId = (reqDTO.getXyGoodsId() != null && !reqDTO.getXyGoodsId().trim().isEmpty()) 
                ? reqDTO.getXyGoodsId().trim() : null;
        String cardContent = (reqDTO.getCardContent() != null && !reqDTO.getCardContent().trim().isEmpty()) 
                ? reqDTO.getCardContent().trim() : null;
        
        Page<CardSecretRespDTO> page = new Page<>(reqDTO.getPageNum(), reqDTO.getPageSize());
        log.info("执行卡密分页查询: accountId={}, goodsId={}, content={}", 
                reqDTO.getXianyuAccountId(), goodsId, cardContent);
                
        return this.baseMapper.selectCardSecretPage(
                page,
                reqDTO.getXianyuAccountId(),
                goodsId,
                reqDTO.getIsUsed(),
                cardContent
        );
    }

    @Override
    @Transactional
    public synchronized String useNextCardSecret(Long accountId, String xyGoodsId, String orderId) {
        LambdaQueryWrapper<XianyuGoodsCardSecret> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XianyuGoodsCardSecret::getXianyuAccountId, accountId)
                .eq(XianyuGoodsCardSecret::getXyGoodsId, xyGoodsId)
                .eq(XianyuGoodsCardSecret::getIsUsed, 0)
                .orderByAsc(XianyuGoodsCardSecret::getId)
                .last("LIMIT 1");
        
        XianyuGoodsCardSecret secret = this.getOne(queryWrapper);
        if (secret != null) {
            secret.setIsUsed(1);
            secret.setOrderId(orderId);
            this.updateById(secret);
            return secret.getCardContent();
        }
        return null;
    }

    @Override
    public long getUnusedCount(Long accountId, String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsCardSecret> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XianyuGoodsCardSecret::getXianyuAccountId, accountId)
                .eq(XianyuGoodsCardSecret::getXyGoodsId, xyGoodsId)
                .eq(XianyuGoodsCardSecret::getIsUsed, 0);
        return this.count(queryWrapper);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            this.removeByIds(ids);
        }
    }
}
