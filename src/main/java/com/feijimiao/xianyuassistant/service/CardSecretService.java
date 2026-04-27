package com.feijimiao.xianyuassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import java.util.List;

public interface CardSecretService extends IService<XianyuGoodsCardSecret> {
    /**
     * 添加卡密
     */
    void addCardSecret(XianyuGoodsCardSecret cardSecret);

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

    /**
     * 分页查询卡密列表
     */
    IPage<CardSecretRespDTO> selectCardSecretPage(CardSecretQueryReqDTO reqDTO);

    /**
     * 批量删除卡密
     */
    void deleteBatch(List<Integer> ids);
}
