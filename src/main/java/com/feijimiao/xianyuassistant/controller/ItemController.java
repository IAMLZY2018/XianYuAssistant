package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {

    @Autowired
    private ItemService itemService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AutoDeliveryService autoDeliveryService;
    
    @Autowired
    private XianyuGoodsAutoReplyRecordMapper autoReplyRecordMapper;

    /**
     * 刷新商品数据
     * 从闲鱼API获取最新商品信息并更新到数据库
     *
     * @param reqDTO 请求参数
     * @return 更新成功的商品ID列表
     */
    @PostMapping("/refresh")
    public ResultObject<RefreshItemsRespDTO> refreshItems(@RequestBody AllItemsReqDTO reqDTO) {
        try {
            log.info("刷新商品数据请求: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            return itemService.refreshItems(reqDTO);
        } catch (Exception e) {
            log.error("刷新商品数据失败", e);
            return ResultObject.failed("刷新商品数据失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库获取商品列表
     *
     * @param reqDTO 请求参数
     * @return 商品列表
     */
    @PostMapping("/list")
    public ResultObject<ItemListFromDbRespDTO> getItemsFromDb(@RequestBody ItemListFromDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品列表: status={}, pageNum={}, pageSize={}", 
                    reqDTO.getStatus(), reqDTO.getPageNum(), reqDTO.getPageSize());
            return itemService.getItemsFromDb(reqDTO);
        } catch (Exception e) {
            log.error("获取数据库商品失败", e);
            return ResultObject.failed("获取数据库商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取商品详情
     *
     * @param reqDTO 请求参数
     * @return 商品详情
     */
    @PostMapping("/detail")
    public ResultObject<ItemDetailRespDTO> getItemDetail(@RequestBody ItemDetailReqDTO reqDTO) {
        try {
            log.info("获取商品详情: xyGoodId={}", reqDTO.getXyGoodId());
            return itemService.getItemDetail(reqDTO);
        } catch (Exception e) {
            log.error("获取商品详情失败", e);
            return ResultObject.failed("获取商品详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新商品自动发货状态
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    @PostMapping("/updateAutoDeliveryStatus")
    public ResultObject<UpdateAutoDeliveryRespDTO> updateAutoDeliveryStatus(@RequestBody UpdateAutoDeliveryReqDTO reqDTO) {
        try {
            log.info("更新商品自动发货状态请求: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoDeliveryOn());
            return itemService.updateAutoDeliveryStatus(reqDTO);
        } catch (Exception e) {
            log.error("更新商品自动发货状态失败", e);
            return ResultObject.failed("更新商品自动发货状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新商品自动回复状态
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    @PostMapping("/updateAutoReplyStatus")
    public ResultObject<UpdateAutoReplyRespDTO> updateAutoReplyStatus(@RequestBody UpdateAutoReplyReqDTO reqDTO) {
        try {
            log.info("更新商品自动回复状态请求: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoReplyOn());
            return itemService.updateAutoReplyStatus(reqDTO);
        } catch (Exception e) {
            log.error("更新商品自动回复状态失败", e);
            return ResultObject.failed("更新商品自动回复状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除商品
     *
     * @param reqDTO 请求参数
     * @return 删除结果
     */
    @PostMapping("/delete")
    public ResultObject<DeleteItemRespDTO> deleteItem(@RequestBody DeleteItemReqDTO reqDTO) {
        try {
            log.info("删除商品请求: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return itemService.deleteItem(reqDTO);
        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ResultObject.failed("删除商品失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取自动发货记录
     *
     * @param reqDTO 请求参数
     * @return 自动发货记录列表
     */
    @PostMapping("/autoDeliveryRecords")
    public ResultObject<AutoDeliveryRecordRespDTO> getAutoDeliveryRecords(@RequestBody AutoDeliveryRecordReqDTO reqDTO) {
        try {
            log.info("获取自动发货记录: xianyuAccountId={}, xyGoodsId={}, pageNum={}, pageSize={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getPageNum(), reqDTO.getPageSize());
            AutoDeliveryRecordRespDTO respDTO = autoDeliveryService.getAutoDeliveryRecords(reqDTO);
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取自动发货记录失败", e);
            return ResultObject.failed("获取自动发货记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取自动回复配置
     *
     * @param reqDTO 请求参数
     * @return 自动回复配置
     */
    @PostMapping("/getRagAutoReplyConfig")
    public ResultObject<RagAutoReplyConfigRespDTO> getRagAutoReplyConfig(@RequestBody RagAutoReplyConfigReqDTO reqDTO) {
        try {
            log.info("获取自动回复配置: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return itemService.getRagAutoReplyConfig(reqDTO);
        } catch (Exception e) {
            log.error("获取自动回复配置失败", e);
            return ResultObject.failed("获取自动回复配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新自动回复配置
     *
     * @param reqDTO 请求参数
     * @return 更新结果
     */
    @PostMapping("/updateRagAutoReplyConfig")
    public ResultObject<?> updateRagAutoReplyConfig(@RequestBody UpdateRagAutoReplyConfigReqDTO reqDTO) {
        try {
            log.info("更新自动回复配置: xianyuAccountId={}, xyGoodsId={}, ragDelaySeconds={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getRagDelaySeconds());
            return itemService.updateRagAutoReplyConfig(reqDTO);
        } catch (Exception e) {
            log.error("更新自动回复配置失败", e);
            return ResultObject.failed("更新自动回复配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取自动回复记录
     */
    @PostMapping("/autoReplyRecords")
    public ResultObject<Map<String, Object>> getAutoReplyRecords(@RequestBody Map<String, Object> params) {
        try {
            Long accountId = Long.valueOf(params.get("xianyuAccountId").toString());
            String xyGoodsId = params.get("xyGoodsId").toString();
            int pageNum = params.containsKey("pageNum") ? Integer.parseInt(params.get("pageNum").toString()) : 1;
            int pageSize = params.containsKey("pageSize") ? Integer.parseInt(params.get("pageSize").toString()) : 20;
            
            int offset = (pageNum - 1) * pageSize;
            List<XianyuGoodsAutoReplyRecord> records = autoReplyRecordMapper.selectByAccountIdAndGoodsId(accountId, xyGoodsId, pageSize, offset);
            int totalCount = autoReplyRecordMapper.countByAccountIdAndGoodsId(accountId, xyGoodsId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", records);
            result.put("totalCount", totalCount);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("获取自动回复记录失败", e);
            return ResultObject.failed("获取自动回复记录失败: " + e.getMessage());
        }
    }
}