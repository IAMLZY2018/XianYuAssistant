package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsCardSecretMapper;
import com.feijimiao.xianyuassistant.service.CardSecretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardSecretServiceImpl extends ServiceImpl<XianyuGoodsCardSecretMapper, XianyuGoodsCardSecret> implements CardSecretService {

    @Override
    @Transactional
    public int importCardSecrets(Long accountId, String xyGoodsId, List<String> secrets) {
        List<XianyuGoodsCardSecret> entities = secrets.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> {
                    XianyuGoodsCardSecret entity = new XianyuGoodsCardSecret();
                    entity.setXianyuAccountId(accountId);
                    entity.setXyGoodsId(xyGoodsId);
                    entity.setCardContent(s.trim());
                    entity.setIsUsed(0);
                    return entity;
                }).collect(Collectors.toList());
        
        if (entities.isEmpty()) return 0;
        this.saveBatch(entities);
        return entities.size();
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
}
