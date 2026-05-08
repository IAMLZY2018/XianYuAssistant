package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSkuProperty;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsSkuPropertyMapper;
import com.feijimiao.xianyuassistant.service.GoodsSkuPropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class GoodsSkuPropertyServiceImpl implements GoodsSkuPropertyService {

    @Autowired
    private XianyuGoodsSkuPropertyMapper mapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<XianyuGoodsSkuProperty> listByXyGoodsId(String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsSkuProperty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuGoodsSkuProperty::getXyGoodsId, xyGoodsId);
        wrapper.orderByAsc(XianyuGoodsSkuProperty::getPropertySortOrder);
        wrapper.orderByAsc(XianyuGoodsSkuProperty::getValueSortOrder);
        return mapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProperties(String xyGoodsId, Long xianyuAccountId, List<XianyuGoodsSkuProperty> propertyList) {
        deleteByXyGoodsId(xyGoodsId);
        String now = LocalDateTime.now().format(FORMATTER);
        for (XianyuGoodsSkuProperty prop : propertyList) {
            prop.setXyGoodsId(xyGoodsId);
            prop.setXianyuAccountId(xianyuAccountId);
            prop.setCreatedTime(now);
            prop.setUpdatedTime(now);
            mapper.insert(prop);
        }
        log.info("保存商品SKU属性维度: xyGoodsId={}, count={}", xyGoodsId, propertyList.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByXyGoodsId(String xyGoodsId) {
        LambdaQueryWrapper<XianyuGoodsSkuProperty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuGoodsSkuProperty::getXyGoodsId, xyGoodsId);
        mapper.delete(wrapper);
    }
}
