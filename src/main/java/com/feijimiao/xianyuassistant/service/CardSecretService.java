package com.feijimiao.xianyuassistant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import java.util.List;

public interface CardSecretService extends IService<XianyuGoodsCardSecret> {
    /**
     * 导入卡密
     */
    int importCardSecrets(Long accountId, String xyGoodsId, List<String> secrets);

    /**
     * 获取下一个未使用的卡密并标记为已使用
     */
    String useNextCardSecret(Long accountId, String xyGoodsId, String orderId);

    /**
     * 获取剩余未使用的卡密数量
     */
    long getUnusedCount(Long accountId, String xyGoodsId);
}
