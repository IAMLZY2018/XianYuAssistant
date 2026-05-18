package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class GoodsBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Override
    public String getModuleKey() {
        return "goods";
    }

    @Override
    public String getModuleName() {
        return "商品管理";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuGoodsInfo> goodsList = goodsInfoMapper.selectList(null);

        List<Map<String, Object>> result = new ArrayList<>();
        for (XianyuGoodsInfo goods : goodsList) {
            XianyuAccount account = accountMapper.selectById(goods.getXianyuAccountId());
            if (account == null) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("unb", account.getUnb());
            map.put("xyGoodId", goods.getXyGoodId());
            map.put("title", goods.getTitle());
            map.put("coverPic", goods.getCoverPic());
            map.put("infoPic", goods.getInfoPic());
            map.put("detailInfo", goods.getDetailInfo());
            map.put("detailUrl", goods.getDetailUrl());
            map.put("soldPrice", goods.getSoldPrice());
            map.put("status", goods.getStatus());
            result.add(map);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("goodsList", result);
        return data;
    }

    @Override
    public void importData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Long> unbToAccountId = context.get("unbToAccountId") != null
                ? (Map<String, Long>) context.get("unbToAccountId")
                : Collections.emptyMap();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> goodsMaps = (List<Map<String, Object>>) data.get("goodsList");
        if (goodsMaps == null) return;

        int skippedCount = 0;
        for (Map<String, Object> map : goodsMaps) {
            try {
                String unb = (String) map.get("unb");
                String xyGoodId = (String) map.get("xyGoodId");
                if (unb == null || xyGoodId == null) continue;

                Long accountId = unbToAccountId.get(unb);
                if (accountId == null) {
                    log.warn("[GoodsBackup] 跳过: 找不到账号, unb={}, xyGoodId={}", unb, xyGoodId);
                    skippedCount++;
                    continue;
                }

                LambdaQueryWrapper<XianyuGoodsInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(XianyuGoodsInfo::getXyGoodId, xyGoodId);
                XianyuGoodsInfo existing = goodsInfoMapper.selectOne(wrapper);

                XianyuGoodsInfo goods = new XianyuGoodsInfo();
                goods.setXianyuAccountId(accountId);
                goods.setXyGoodId(xyGoodId);
                goods.setTitle((String) map.get("title"));
                goods.setCoverPic((String) map.get("coverPic"));
                goods.setInfoPic((String) map.get("infoPic"));
                goods.setDetailInfo((String) map.get("detailInfo"));
                goods.setDetailUrl((String) map.get("detailUrl"));
                goods.setSoldPrice((String) map.get("soldPrice"));
                goods.setStatus(map.get("status") != null ? ((Number) map.get("status")).intValue() : null);

                if (existing == null) {
                    goodsInfoMapper.insert(goods);
                } else {
                    goods.setId(existing.getId());
                    goodsInfoMapper.updateById(goods);
                }
            } catch (Exception e) {
                log.warn("[GoodsBackup] 导入单条商品数据失败: {}", e.getMessage());
            }
        }
        if (skippedCount > 0) {
            log.warn("[GoodsBackup] 共跳过 {} 条数据（账号不存在）", skippedCount);
        }
    }
}
