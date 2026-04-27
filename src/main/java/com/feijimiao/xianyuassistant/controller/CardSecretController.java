package com.feijimiao.xianyuassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretImportDTO;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.CardSecretRespDTO;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsCardSecret;
import com.feijimiao.xianyuassistant.service.CardSecretService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卡密管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/card-secret")
@CrossOrigin(origins = "*")
public class CardSecretController {

    @Autowired
    private CardSecretService cardSecretService;

    /**
     * 分页查询卡密列表
     */
    @PostMapping("/list")
    public ResultObject<IPage<CardSecretRespDTO>> list(@RequestBody CardSecretQueryReqDTO reqDTO) {
        log.info("查询卡密列表请求: {}", reqDTO);
        try {
            return ResultObject.success(cardSecretService.selectCardSecretPage(reqDTO));
        } catch (Exception e) {
            log.error("查询卡密列表失败", e);
            return ResultObject.failed("查询卡密列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除卡密
     */
    @PostMapping("/delete")
    public ResultObject<Void> delete(@RequestParam("id") Integer id) {
        try {
            cardSecretService.removeById(id);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("删除卡密失败", e);
            return ResultObject.failed("删除卡密失败: " + e.getMessage());
        }
    }

    /**
     * 添加卡密
     */
    @PostMapping("/add")
    public ResultObject<Void> add(@RequestBody XianyuGoodsCardSecret cardSecret) {
        try {
            cardSecretService.addCardSecret(cardSecret);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("添加卡密失败", e);
            return ResultObject.failed("添加卡密失败: " + e.getMessage());
        }
    }

    /**
     * 更新卡密
     */
    @PostMapping("/update")
    public ResultObject<Void> update(@RequestBody XianyuGoodsCardSecret cardSecret) {
        try {
            cardSecretService.updateById(cardSecret);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("更新卡密失败", e);
            return ResultObject.failed("更新卡密失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除卡密
     */
    @PostMapping("/batch-delete")
    public ResultObject<Void> batchDelete(@RequestBody List<Integer> ids) {
        try {
            cardSecretService.deleteBatch(ids);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("批量删除卡密失败", e);
            return ResultObject.failed("批量删除卡密失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入卡密
     */
    @PostMapping("/import")
    public ResultObject<Integer> batchImport(@RequestBody CardSecretImportDTO importDTO) {
        try {
            int count = cardSecretService.importCardSecrets(
                    importDTO.getXianyuAccountId(),
                    importDTO.getXyGoodsId(),
                    importDTO.getSecrets()
            );
            return ResultObject.success(count);
        } catch (Exception e) {
            log.error("导入卡密失败", e);
            return ResultObject.failed("导入卡密失败: " + e.getMessage());
        }
    }
}
