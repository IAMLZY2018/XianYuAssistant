package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSku;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsSkuMapper;
import com.feijimiao.xianyuassistant.service.GoodsSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class GoodsSkuServiceImpl implements GoodsSkuService {

    @Autowired
    private XianyuGoodsSkuMapper goodsSkuMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String getCurrentTimeString() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    @Override
    public List<XianyuGoodsSku> listByXyGoodsId(String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuGoodsSku::getXyGoodsId, xyGoodsId);
        wrapper.orderByAsc(XianyuGoodsSku::getSkuId);
        return goodsSkuMapper.selectList(wrapper);
    }

    @Override
    public int countByXyGoodsId(String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuGoodsSku::getXyGoodsId, xyGoodsId);
        return Math.toIntExact(goodsSkuMapper.selectCount(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkus(String xyGoodsId, Long xianyuAccountId, List<XianyuGoodsSku> skuList) {
        deleteByXyGoodsId(xyGoodsId);

        String now = getCurrentTimeString();
        for (XianyuGoodsSku sku : skuList) {
            sku.setXyGoodsId(xyGoodsId);
            sku.setXianyuAccountId(xianyuAccountId);
            sku.setCreatedTime(now);
            sku.setUpdatedTime(now);
            goodsSkuMapper.insert(sku);
        }
        log.info("保存商品SKU: xyGoodsId={}, count={}", xyGoodsId, skuList.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByXyGoodsId(String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuGoodsSku::getXyGoodsId, xyGoodsId);
        goodsSkuMapper.delete(wrapper);
    }
}
