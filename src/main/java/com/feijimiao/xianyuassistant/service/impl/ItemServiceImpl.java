package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.service.ItemService;
import com.feijimiao.xianyuassistant.utils.XianyuApiUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import com.feijimiao.xianyuassistant.utils.ItemDetailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AccountService accountService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.GoodsInfoService goodsInfoService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AutoDeliveryService autoDeliveryService;

    /**
     * 获取指定页的商品信息（内部方法）
     */
    private ResultObject<ItemListRespDTO> getItemList(ItemListReqDTO reqDTO) {
        try {
            log.info("开始获取商品列表: {}", reqDTO);

            // 从数据库获取Cookie
            String cookiesStr = getCookieFromDb(reqDTO.getCookieId());
            if (cookiesStr == null || cookiesStr.isEmpty()) {
                log.error("未找到账号Cookie: cookieId={}", reqDTO.getCookieId());
                return ResultObject.failed("未找到账号Cookie");
            }
            log.info("Cookie获取成功，长度: {}", cookiesStr.length());

            // 检查Cookie中是否包含必需的token
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            if (!cookies.containsKey("_m_h5_tk") || cookies.get("_m_h5_tk").isEmpty()) {
                log.error("Cookie中缺少_m_h5_tk字段！请重新登录");
                return ResultObject.failed("Cookie中缺少_m_h5_tk，请重新登录");
            }
            
            // 构建请求数据
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("needGroupInfo", false);
            dataMap.put("pageNumber", reqDTO.getPageNumber());
            dataMap.put("pageSize", reqDTO.getPageSize());
            dataMap.put("groupName", "在售");
            dataMap.put("groupId", "58877261");
            dataMap.put("defaultGroup", true);
            dataMap.put("userId", cookies.get("unb"));
            
            log.info("调用商品列表API: pageNumber={}, pageSize={}", reqDTO.getPageNumber(), reqDTO.getPageSize());
            
            // 使用工具类调用API
            String response = XianyuApiUtils.callApi(
                "mtop.idle.web.xyh.item.list",
                dataMap,
                cookiesStr,
                "a21ybx.im.0.0",
                "a21ybx.collection.menu.1.272b5141NafCNK"
            );
            
            if (response == null) {
                log.error("API调用失败：响应为空");
                return ResultObject.failed("请求闲鱼API失败");
            }
            
            log.info("API调用成功，响应长度: {}", response.length());

            // 解析响应
            log.info("开始解析响应JSON...");
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            log.info("JSON解析成功，响应字段: {}", responseMap.keySet());
            
            ItemListRespDTO respDTO = parseItemListResponse(responseMap, reqDTO.getPageNumber(), reqDTO.getPageSize());
            log.info("响应解析完成，success={}, 商品数量={}", respDTO.getSuccess(), respDTO.getCurrentCount());
            
            if (respDTO.getSuccess()) {
                log.info("获取商品列表成功: cookieId={}, 商品数量={}", 
                        reqDTO.getCookieId(), respDTO.getCurrentCount());
                
                // 保存商品信息到数据库
                if (respDTO.getItems() != null && !respDTO.getItems().isEmpty()) {
                    try {
                        // 获取账号ID
                        Long accountId = getAccountIdFromCookieId(reqDTO.getCookieId());
                        int savedCount = goodsInfoService.batchSaveOrUpdateGoodsInfo(respDTO.getItems(), accountId);
                        log.info("商品信息已保存到数据库: 成功数量={}, accountId={}", savedCount, accountId);
                    } catch (Exception e) {
                        log.error("保存商品信息到数据库失败", e);
                        // 不影响主流程，继续返回结果
                    }
                }
                
                return ResultObject.success(respDTO);
            } else {
                log.error("获取商品列表失败: success=false");
                return ResultObject.failed("获取商品列表失败");
            }
        } catch (com.feijimiao.xianyuassistant.exception.BusinessException e) {
            log.error("业务异常: cookieId={}, message={}", reqDTO.getCookieId(), e.getMessage());
            
            // 如果是令牌过期异常，更新cookie状态
            if (e.getMessage().contains("令牌已过期")) {
                try {
                    Long accountId = getAccountIdFromCookieId(reqDTO.getCookieId());
                    if (accountId != null) {
                        accountService.updateCookieStatus(accountId, 2); // 2表示过期
                        log.info("已更新Cookie状态为过期: accountId={}", accountId);
                    }
                } catch (Exception ex) {
                    log.error("更新Cookie状态失败: cookieId={}", reqDTO.getCookieId(), ex);
                }
            }
            
            // 重新抛出业务异常，让全局异常处理器处理
            throw e;
        } catch (Exception e) {
            log.error("获取商品列表异常: cookieId={}", reqDTO.getCookieId(), e);
            return ResultObject.failed("获取商品列表异常: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<RefreshItemsRespDTO> refreshItems(AllItemsReqDTO reqDTO) {
        try {
            log.info("开始刷新商品数据: xianyuAccountId={}", reqDTO.getXianyuAccountId());
            
            // 验证账号ID
            if (reqDTO.getXianyuAccountId() == null) {
                log.error("账号ID不能为空");
                return ResultObject.failed("账号ID不能为空");
            }
            
            // 根据账号ID获取Cookie
            String cookieStr = accountService.getCookieByAccountId(reqDTO.getXianyuAccountId());
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("未找到账号Cookie: xianyuAccountId={}", reqDTO.getXianyuAccountId());
                return ResultObject.failed("未找到账号Cookie，请先登录");
            }
            
            log.info("获取账号Cookie成功: xianyuAccountId={}", reqDTO.getXianyuAccountId());

            RefreshItemsRespDTO respDTO = new RefreshItemsRespDTO();
            respDTO.setSuccess(false);
            respDTO.setUpdatedItemIds(new ArrayList<>());
            
            List<ItemDTO> allItems = new ArrayList<>();
            int pageNumber = 1;

            // 自动分页获取所有商品
            while (true) {
                // 检查是否达到最大页数（maxPages为null或0表示不限制）
                if (reqDTO.getMaxPages() != null && reqDTO.getMaxPages() > 0 && pageNumber > reqDTO.getMaxPages()) {
                    log.info("达到最大页数限制: {}", reqDTO.getMaxPages());
                    break;
                }

                // 获取当前页
                ItemListReqDTO pageReqDTO = new ItemListReqDTO();
                pageReqDTO.setCookieId(String.valueOf(reqDTO.getXianyuAccountId()));
                pageReqDTO.setPageNumber(pageNumber);
                pageReqDTO.setPageSize(reqDTO.getPageSize());

                ResultObject<ItemListRespDTO> pageResult = getItemList(pageReqDTO);
                
                if (pageResult.getCode() != 200 || pageResult.getData() == null || !pageResult.getData().getSuccess()) {
                    log.error("获取第{}页失败", pageNumber);
                    // 如果是第一页就失败了，返回错误
                    if (pageNumber == 1) {
                        return ResultObject.failed(pageResult.getMsg() != null ? pageResult.getMsg() : "获取商品列表失败");
                    }
                    // 如果不是第一页，继续处理已获取的数据
                    break;
                }

                ItemListRespDTO pageData = pageResult.getData();
                if (pageData.getItems() == null || pageData.getItems().isEmpty()) {
                    log.info("第{}页没有数据，刷新完成", pageNumber);
                    break;
                }

                allItems.addAll(pageData.getItems());
                log.info("第{}页获取到{}个商品", pageNumber, pageData.getItems().size());

                // 如果当前页商品数量少于页面大小，说明已经是最后一页
                if (pageData.getItems().size() < reqDTO.getPageSize()) {
                    log.info("第{}页商品数量({})少于页面大小({})，刷新完成", 
                            pageNumber, pageData.getItems().size(), reqDTO.getPageSize());
                    break;
                }

                pageNumber++;
                
                // 模拟人工翻页延迟（800ms - 2000ms）
                log.debug("模拟人工翻页延迟...");
                com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.pageScrollDelay();
            }

            // 批量保存到数据库
            respDTO.setTotalCount(allItems.size());
            
            if (!allItems.isEmpty()) {
                // 使用账号ID保存商品
                Long accountId = reqDTO.getXianyuAccountId();
                
                // 保存商品并收集成功的商品ID
                for (ItemDTO item : allItems) {
                    try {
                        if (goodsInfoService.saveOrUpdateGoodsInfo(item, accountId)) {
                            if (item.getDetailParams() != null && item.getDetailParams().getItemId() != null) {
                                respDTO.getUpdatedItemIds().add(item.getDetailParams().getItemId());
                            }
                        }
                    } catch (Exception e) {
                        log.error("保存商品失败: itemId={}", 
                                item.getDetailParams() != null ? item.getDetailParams().getItemId() : "null", e);
                    }
                }
                
                respDTO.setSuccessCount(respDTO.getUpdatedItemIds().size());
                respDTO.setSuccess(true);
                respDTO.setMessage("刷新成功");
                
                log.info("刷新商品数据完成: xianyuAccountId={}, 总数={}, 成功={}", 
                        reqDTO.getXianyuAccountId(), respDTO.getTotalCount(), respDTO.getSuccessCount());
            } else {
                respDTO.setSuccessCount(0);
                respDTO.setMessage("没有获取到商品数据");
                log.warn("刷新商品数据完成，但没有获取到任何商品");
            }

            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("刷新商品数据异常: xianyuAccountId={}", reqDTO.getXianyuAccountId(), e);
            return ResultObject.failed("刷新商品数据异常: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<ItemListFromDbRespDTO> getItemsFromDb(ItemListFromDbReqDTO reqDTO) {
        try {
            log.info("从数据库获取商品列表: status={}, xianyuAccountId={}, pageNum={}, pageSize={}", 
                    reqDTO.getStatus(), reqDTO.getXianyuAccountId(), reqDTO.getPageNum(), reqDTO.getPageSize());
            
            // 获取分页参数
            int pageSize = reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20;
            int pageNum = reqDTO.getPageNum() != null ? reqDTO.getPageNum() : 1;
            
            // 确保页码有效
            if (pageNum < 1) {
                pageNum = 1;
            }
            
            // 统计总数（使用count查询，提高性能）
            int totalCount = goodsInfoService.countByStatusAndAccountId(reqDTO.getStatus(), reqDTO.getXianyuAccountId());
            
            // 计算总页数
            int totalPage = (int) Math.ceil((double) totalCount / pageSize);
            
            // 如果总页数为0，设置为1
            if (totalPage == 0) {
                totalPage = 1;
            }
            
            // 确保页码不超过总页数
            if (pageNum > totalPage && totalPage > 0) {
                pageNum = totalPage;
            }
            
            // 获取当前页的商品列表（带账号ID筛选）
            List<XianyuGoodsInfo> pagedItems = goodsInfoService.listByStatusAndAccountId(
                    reqDTO.getStatus(), reqDTO.getXianyuAccountId(), pageNum, pageSize);
            
            // 如果分页查询结果为null，创建空列表
            if (pagedItems == null) {
                pagedItems = new ArrayList<>();
            }
            
            // 为每个商品添加配置信息
            List<ItemWithConfigDTO> itemsWithConfig = new ArrayList<>();
            for (XianyuGoodsInfo item : pagedItems) {
                ItemWithConfigDTO itemWithConfig = new ItemWithConfigDTO();
                itemWithConfig.setItem(item);
                
                // 获取商品配置
                if (item.getXianyuAccountId() != null) {
                    com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig config = 
                            autoDeliveryService.getGoodsConfig(item.getXianyuAccountId(), item.getXyGoodId());
                    
                    if (config != null) {
                        itemWithConfig.setXianyuAutoDeliveryOn(config.getXianyuAutoDeliveryOn());
                        itemWithConfig.setXianyuAutoReplyOn(config.getXianyuAutoReplyOn());
                    } else {
                        itemWithConfig.setXianyuAutoDeliveryOn(0);
                        itemWithConfig.setXianyuAutoReplyOn(0);
                    }
                } else {
                    itemWithConfig.setXianyuAutoDeliveryOn(0);
                    itemWithConfig.setXianyuAutoReplyOn(0);
                }
                
                itemsWithConfig.add(itemWithConfig);
            }
            
            ItemListFromDbRespDTO respDTO = new ItemListFromDbRespDTO();
            respDTO.setItemsWithConfig(itemsWithConfig);
            respDTO.setTotalCount(totalCount);
            respDTO.setPageNum(pageNum);
            respDTO.setPageSize(pageSize);
            respDTO.setTotalPage(totalPage);
            
            // 添加调试日志
            log.info("分页信息: totalCount={}, pageNum={}, pageSize={}, totalPage={}", 
                    totalCount, pageNum, pageSize, totalPage);
            
            log.info("从数据库获取商品列表成功: 总数={}, 当前页={}, 每页={}, 总页数={}", 
                    totalCount, pageNum, pageSize, totalPage);
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("从数据库获取商品列表失败", e);
            return ResultObject.failed("获取商品列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<ItemDetailRespDTO> getItemDetail(ItemDetailReqDTO reqDTO) {
        try {
            log.info("获取商品详情: xyGoodId={}, cookieId={}", reqDTO.getXyGoodId(), reqDTO.getCookieId());
            
            // 1. 从数据库获取商品基本信息
            XianyuGoodsInfo item = goodsInfoService.getByXyGoodId(reqDTO.getXyGoodId());
            
            if (item == null) {
                return ResultObject.failed("商品不存在");
            }
            
            // 2. 判断是否需要获取详情
            boolean needFetchDetail = false;
            
            // 2.1 如果 detail_info 为空，必须获取
            if (item.getDetailInfo() == null || item.getDetailInfo().isEmpty()) {
                log.info("商品详情为空，需要获取: xyGoodId={}", reqDTO.getXyGoodId());
                needFetchDetail = true;
            }
            // 2.2 如果提供了 cookieId，也尝试获取/更新
            else if (reqDTO.getCookieId() != null && !reqDTO.getCookieId().isEmpty()) {
                log.info("提供了cookieId，尝试更新商品详情: xyGoodId={}", reqDTO.getXyGoodId());
                needFetchDetail = true;
            }
            
            // 3. 如果需要获取详情
            if (needFetchDetail) {
                // 3.1 确定使用哪个cookieId
                String cookieIdToUse = reqDTO.getCookieId();
                
                // 3.2 如果没有提供 cookieId，尝试从商品的 xianyu_account_id 获取
                if (cookieIdToUse == null || cookieIdToUse.isEmpty()) {
                    if (item.getXianyuAccountId() != null) {
                        // 使用商品关联的账号ID
                        cookieIdToUse = String.valueOf(item.getXianyuAccountId());
                        log.info("使用商品关联的账号ID获取详情: xyGoodId={}, accountId={}", 
                                reqDTO.getXyGoodId(), item.getXianyuAccountId());
                    } else {
                        log.warn("商品未关联账号且未提供cookieId，无法获取详情: xyGoodId={}", reqDTO.getXyGoodId());
                        ItemDetailRespDTO respDTO = new ItemDetailRespDTO();
                        respDTO.setItemWithConfig(buildItemWithConfig(item));
                        return ResultObject.failed("商品详情为空，且商品未关联账号，请提供cookieId参数以获取详情");
                    }
                }
                
                // 3.2 调用API获取详情
                try {
                    String detailInfo = fetchItemDetailFromApi(reqDTO.getXyGoodId(), cookieIdToUse);
                    
                    if (detailInfo != null && !detailInfo.isEmpty()) {
                        // 更新数据库中的详情信息
                        goodsInfoService.updateDetailInfo(reqDTO.getXyGoodId(), detailInfo);
                        item.setDetailInfo(detailInfo);
                        log.info("商品详情已更新: xyGoodId={}", reqDTO.getXyGoodId());
                    } else {
                        log.warn("未能获取到商品详情: xyGoodId={}", reqDTO.getXyGoodId());
                    }
                } catch (Exception e) {
                    log.error("获取商品详情失败，返回数据库中的信息: xyGoodId={}", reqDTO.getXyGoodId(), e);
                    // 即使获取详情失败，也返回数据库中的基本信息
                }
            }
            
            ItemDetailRespDTO respDTO = new ItemDetailRespDTO();
            respDTO.setItemWithConfig(buildItemWithConfig(item));
            
            log.info("获取商品详情成功: xyGoodId={}", reqDTO.getXyGoodId());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取商品详情失败: xyGoodId={}", reqDTO.getXyGoodId(), e);
            return ResultObject.failed("获取商品详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建包含配置的商品信息
     */
    private ItemWithConfigDTO buildItemWithConfig(XianyuGoodsInfo item) {
        ItemWithConfigDTO itemWithConfig = new ItemWithConfigDTO();
        itemWithConfig.setItem(item);
        
        // 获取商品配置
        if (item.getXianyuAccountId() != null) {
            com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig config = 
                    autoDeliveryService.getGoodsConfig(item.getXianyuAccountId(), item.getXyGoodId());
            
            if (config != null) {
                itemWithConfig.setXianyuAutoDeliveryOn(config.getXianyuAutoDeliveryOn());
                itemWithConfig.setXianyuAutoReplyOn(config.getXianyuAutoReplyOn());
            } else {
                itemWithConfig.setXianyuAutoDeliveryOn(0);
                itemWithConfig.setXianyuAutoReplyOn(0);
            }
            
            // 获取自动发货配置
            com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig deliveryConfig = 
                    autoDeliveryService.getAutoDeliveryConfig(item.getXianyuAccountId(), item.getXyGoodId());
            
            if (deliveryConfig != null) {
                itemWithConfig.setAutoDeliveryType(deliveryConfig.getType());
                itemWithConfig.setAutoDeliveryContent(deliveryConfig.getAutoDeliveryContent());
            }
        } else {
            itemWithConfig.setXianyuAutoDeliveryOn(0);
            itemWithConfig.setXianyuAutoReplyOn(0);
        }
        
        return itemWithConfig;
    }
    
    /**
     * 从闲鱼API获取商品详情
     * 实现流程：
     * 1. 检查缓存（24小时内的详情不重复获取）
     * 2. 首选：通过闲鱼API mtop.taobao.idle.pc.detail 获取
     * 3. 备选：如果API失败，可以考虑使用浏览器访问（需要额外实现）
     *
     * @param itemId 商品ID
     * @param cookieId Cookie ID
     * @return 商品详情JSON字符串
     */
    private String fetchItemDetailFromApi(String itemId, String cookieId) {
        try {
            log.info("开始获取商品详情: itemId={}, cookieId={}", itemId, cookieId);
            
            // 1. 检查缓存：如果数据库中已有详情且在24小时内，直接返回
            XianyuGoodsInfo cachedItem = goodsInfoService.getByXyGoodId(itemId);
            if (cachedItem != null && cachedItem.getDetailInfo() != null && !cachedItem.getDetailInfo().isEmpty()) {
                // 检查更新时间是否在24小时内
                if (isDetailInfoFresh(cachedItem.getUpdatedTime())) {
                    log.info("使用缓存的商品详情: itemId={}, 缓存时间={}", itemId, cachedItem.getUpdatedTime());
                    return cachedItem.getDetailInfo();
                } else {
                    log.info("缓存的商品详情已过期，重新获取: itemId={}", itemId);
                }
            } else {
                log.info("数据库中没有商品详情缓存，需要调用API获取: itemId={}", itemId);
            }
            
            // 2. 从数据库获取Cookie
            String cookiesStr = getCookieFromDb(cookieId);
            if (cookiesStr == null || cookiesStr.isEmpty()) {
                log.error("未找到账号Cookie: cookieId={}", cookieId);
                return null;
            }
            
            log.info("Cookie获取成功，准备调用API: itemId={}", itemId);
            
            // 3. 首选方式：通过闲鱼API获取商品详情
            String detailJson = fetchDetailFromApi(itemId, cookiesStr);
            
            if (detailJson != null && !detailJson.isEmpty()) {
                log.info("通过API获取商品详情成功: itemId={}, 详情长度={}", itemId, detailJson.length());
                return detailJson;
            }
            
            // 4. 备选方式：通过浏览器访问获取（暂未实现）
            log.warn("API获取商品详情失败，备选方式（浏览器访问）暂未实现: itemId={}", itemId);
            
            // 如果有缓存的详情（即使过期），也返回它
            if (cachedItem != null && cachedItem.getDetailInfo() != null && !cachedItem.getDetailInfo().isEmpty()) {
                log.info("返回过期的缓存详情: itemId={}", itemId);
                return cachedItem.getDetailInfo();
            }
            
            log.error("无法获取商品详情，且没有可用的缓存: itemId={}", itemId);
            return null;
            
        } catch (Exception e) {
            log.error("获取商品详情异常: itemId={}", itemId, e);
            return null;
        }
    }
    
    /**
     * 通过闲鱼API获取商品详情
     *
     * @param itemId 商品ID
     * @param cookiesStr Cookie字符串
     * @return 商品详情JSON字符串
     */
    private String fetchDetailFromApi(String itemId, String cookiesStr) {
        try {
            log.info("调用闲鱼API获取商品详情: itemId={}", itemId);
            
            // 构建请求数据
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("itemId", itemId);
            
            // 调用闲鱼API
            String response = XianyuApiUtils.callApi(
                "mtop.taobao.idle.pc.detail",
                dataMap,
                cookiesStr
            );
            
            if (response == null) {
                log.error("API调用失败：响应为空, itemId={}", itemId);
                return null;
            }
            
            log.info("API响应成功，响应长度: {}, itemId={}", response.length(), itemId);
            
            // 检查响应是否成功
            if (!XianyuApiUtils.isSuccess(response)) {
                String error = XianyuApiUtils.extractError(response);
                log.error("API返回失败: {}, itemId={}", error, itemId);
                // 打印完整响应用于调试
                log.error("完整响应内容: {}", response);
                return null;
            }
            
            log.info("API响应状态检查通过，开始提取data字段: itemId={}", itemId);
            
            // 提取data字段
            Map<String, Object> data = XianyuApiUtils.extractData(response);
            if (data == null) {
                log.error("无法提取data字段, itemId={}", itemId);
                log.error("响应内容: {}", response);
                return null;
            }
            
            log.info("data字段提取成功，包含 {} 个字段, itemId={}", data.size(), itemId);
            
            // 将data转换为JSON字符串
            String detailJson = objectMapper.writeValueAsString(data);
            log.info("API获取商品详情成功: itemId={}, 详情长度={}", itemId, detailJson.length());
            
            // 提取desc字段
            String extractedDesc = ItemDetailUtils.extractDescFromDetailJson(detailJson);
            log.info("提取desc字段成功: itemId={}, 原始长度={}, 提取后长度={}", 
                    itemId, detailJson.length(), extractedDesc.length());
            
            return extractedDesc;
            
        } catch (Exception e) {
            log.error("API获取商品详情异常: itemId={}", itemId, e);
            return null;
        }
    }
    
    /**
     * 检查详情信息是否新鲜（24小时内）
     *
     * @param updatedTime 更新时间字符串（格式：yyyy-MM-dd HH:mm:ss）
     * @return 是否新鲜
     */
    private boolean isDetailInfoFresh(String updatedTime) {
        if (updatedTime == null || updatedTime.isEmpty()) {
            return false;
        }
        
        try {
            // 解析更新时间
            java.time.LocalDateTime updateDateTime = java.time.LocalDateTime.parse(
                updatedTime, 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            
            // 计算时间差
            java.time.Duration duration = java.time.Duration.between(updateDateTime, java.time.LocalDateTime.now());
            long hours = duration.toHours();
            
            // 24小时内认为是新鲜的
            boolean isFresh = hours < 24;
            log.debug("详情缓存检查: 更新时间={}, 距今{}小时, 是否新鲜={}", updatedTime, hours, isFresh);
            
            return isFresh;
            
        } catch (Exception e) {
            log.error("解析更新时间失败: {}", updatedTime, e);
            return false;
        }
    }

    /**
     * 解析商品列表响应
     */
    @SuppressWarnings("unchecked")
    private ItemListRespDTO parseItemListResponse(Map<String, Object> responseMap, int pageNumber, int pageSize) {
        ItemListRespDTO respDTO = new ItemListRespDTO();
        respDTO.setPageNumber(pageNumber);
        respDTO.setPageSize(pageSize);
        respDTO.setItems(new ArrayList<>());

        try {
            log.info("开始解析响应，responseMap keys: {}", responseMap.keySet());
            
            List<String> ret = (List<String>) responseMap.get("ret");
            log.info("ret字段: {}", ret);
            
            // 检查令牌是否过期
            if (ret != null && !ret.isEmpty()) {
                String retValue = ret.get(0);
                if (retValue.contains("FAIL_SYS_TOKEN_EXOIRED") || retValue.contains("令牌过期")) {
                    log.warn("API调用失败，ret: {}", ret);
                    throw new com.feijimiao.xianyuassistant.exception.BusinessException("令牌已过期，请重新登录");
                }
            }
            
            if (ret != null && !ret.isEmpty() && ret.get(0).contains("SUCCESS")) {
                log.info("API调用成功，开始解析数据");
                respDTO.setSuccess(true);

                Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                log.info("data字段存在: {}, keys: {}", data != null, data != null ? data.keySet() : "null");
                
                if (data != null) {
                    List<Map<String, Object>> cardList = (List<Map<String, Object>>) data.get("cardList");
                    log.info("cardList存在: {}, size: {}", cardList != null, cardList != null ? cardList.size() : 0);
                    
                    if (cardList != null) {
                        for (Map<String, Object> card : cardList) {
                            Map<String, Object> cardData = (Map<String, Object>) card.get("cardData");
                            if (cardData != null) {
                                // 将 Map 转换为 ItemDTO
                                ItemDTO itemDTO = objectMapper.convertValue(cardData, ItemDTO.class);
                                respDTO.getItems().add(itemDTO);
                            }
                        }
                    }
                }

                respDTO.setCurrentCount(respDTO.getItems().size());
                respDTO.setSavedCount(respDTO.getItems().size());
                log.info("解析完成，商品数量: {}", respDTO.getItems().size());
            } else {
                log.warn("API调用失败，ret: {}", ret);
                respDTO.setSuccess(false);
            }
        } catch (com.feijimiao.xianyuassistant.exception.BusinessException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("解析商品列表响应失败", e);
            respDTO.setSuccess(false);
        }

        return respDTO;
    }

    /**
     * 从cookieId获取账号ID
     * cookieId可以是：账号ID、账号备注(account_note)或UNB
     *
     * @param cookieId Cookie ID
     * @return 账号ID
     */
    private Long getAccountIdFromCookieId(String cookieId) {
        try {
            // 1. 先尝试作为账号ID解析（数字）
            try {
                return Long.parseLong(cookieId);
            } catch (NumberFormatException e) {
                // 不是数字，继续其他方式查询
                log.debug("cookieId不是数字，尝试其他查询方式: {}", cookieId);
            }
            
            // 2. 尝试按账号备注查询
            Long accountId = accountService.getAccountIdByAccountNote(cookieId);
            if (accountId != null) {
                log.info("通过账号备注获取账号ID成功: accountNote={}, accountId={}", cookieId, accountId);
                return accountId;
            }
            
            // 3. 尝试按UNB查询
            accountId = accountService.getAccountIdByUnb(cookieId);
            if (accountId != null) {
                log.info("通过UNB获取账号ID成功: unb={}, accountId={}", cookieId, accountId);
                return accountId;
            }
            
            log.warn("未找到账号ID: cookieId={}", cookieId);
            return null;
            
        } catch (Exception e) {
            log.error("获取账号ID失败: cookieId={}", cookieId, e);
            return null;
        }
    }
    
    /**
     * 从数据库获取Cookie（包含 m_h5_tk 补充逻辑）
     * cookieId可以是：账号ID、账号备注(account_note)或UNB
     */
    private String getCookieFromDb(String cookieId) {
        try {
            log.info("从数据库查询Cookie: cookieId={}", cookieId);
            
            String cookie = null;
            
            // 1. 先尝试作为账号ID查询（数字）
            try {
                Long accountId = Long.parseLong(cookieId);
                cookie = accountService.getCookieByAccountId(accountId);
                if (cookie != null) {
                    log.info("通过账号ID获取Cookie成功: accountId={}", accountId);
                    // 检查并补充 _m_h5_tk
                    cookie = ensureMh5tkInCookie(cookie, accountId);
                    return cookie;
                }
            } catch (NumberFormatException e) {
                // 不是数字，继续其他方式查询
                log.debug("cookieId不是数字，尝试其他查询方式: {}", cookieId);
            }
            
            // 2. 尝试按账号备注查询
            cookie = accountService.getCookieByAccountNote(cookieId);
            if (cookie != null) {
                log.info("通过账号备注获取Cookie成功: accountNote={}", cookieId);
                return cookie;
            }
            
            // 3. 尝试按UNB查询
            cookie = accountService.getCookieByUnb(cookieId);
            if (cookie != null) {
                log.info("通过UNB获取Cookie成功: unb={}", cookieId);
                return cookie;
            }
            
            log.warn("未找到Cookie: cookieId={}", cookieId);
            return null;
            
        } catch (Exception e) {
            log.error("从数据库获取Cookie失败: cookieId={}", cookieId, e);
            return null;
        }
    }
    
    @Override
    public ResultObject<UpdateAutoDeliveryRespDTO> updateAutoDeliveryStatus(UpdateAutoDeliveryReqDTO reqDTO) {
        try {
            log.info("更新商品自动发货状态: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoDeliveryOn());
            
            // 1. 获取商品配置
            com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig goodsConfig = 
                    autoDeliveryService.getGoodsConfig(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            // 2. 如果配置不存在，创建新的配置
            if (goodsConfig == null) {
                goodsConfig = new com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig();
                goodsConfig.setXianyuAccountId(reqDTO.getXianyuAccountId());
                goodsConfig.setXyGoodsId(reqDTO.getXyGoodsId());
                goodsConfig.setXianyuAutoDeliveryOn(reqDTO.getXianyuAutoDeliveryOn());
                goodsConfig.setXianyuAutoReplyOn(0); // 默认关闭自动回复
                goodsConfig.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                goodsConfig.setUpdateTime(goodsConfig.getCreateTime());
            } else {
                // 3. 更新配置
                goodsConfig.setXianyuAutoDeliveryOn(reqDTO.getXianyuAutoDeliveryOn());
                goodsConfig.setUpdateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            }
            
            // 4. 保存配置
            autoDeliveryService.saveOrUpdateGoodsConfig(goodsConfig);
            
            // 5. 返回结果
            UpdateAutoDeliveryRespDTO respDTO = new UpdateAutoDeliveryRespDTO();
            respDTO.setSuccess(true);
            respDTO.setMessage("自动发货状态更新成功");
            
            log.info("自动发货状态更新成功: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoDeliveryOn());
            
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("更新自动发货状态失败: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), e);
            return ResultObject.failed("更新自动发货状态失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<UpdateAutoReplyRespDTO> updateAutoReplyStatus(UpdateAutoReplyReqDTO reqDTO) {
        try {
            log.info("更新商品自动回复状态: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoReplyOn());
            
            // 1. 获取商品配置
            com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig goodsConfig = 
                    autoDeliveryService.getGoodsConfig(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            // 2. 如果配置不存在，创建新的配置
            if (goodsConfig == null) {
                goodsConfig = new com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig();
                goodsConfig.setXianyuAccountId(reqDTO.getXianyuAccountId());
                goodsConfig.setXyGoodsId(reqDTO.getXyGoodsId());
                goodsConfig.setXianyuAutoDeliveryOn(0); // 默认关闭自动发货
                goodsConfig.setXianyuAutoReplyOn(reqDTO.getXianyuAutoReplyOn());
                goodsConfig.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                goodsConfig.setUpdateTime(goodsConfig.getCreateTime());
            } else {
                // 3. 更新配置
                goodsConfig.setXianyuAutoReplyOn(reqDTO.getXianyuAutoReplyOn());
                goodsConfig.setUpdateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
            }
            
            // 4. 保存配置
            autoDeliveryService.saveOrUpdateGoodsConfig(goodsConfig);
            
            // 5. 返回结果
            UpdateAutoReplyRespDTO respDTO = new UpdateAutoReplyRespDTO();
            respDTO.setSuccess(true);
            respDTO.setMessage("自动回复状态更新成功");
            
            log.info("自动回复状态更新成功: xianyuAccountId={}, xyGoodsId={}, status={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getXianyuAutoReplyOn());
            
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("更新自动回复状态失败: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), e);
            return ResultObject.failed("更新自动回复状态失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<DeleteItemRespDTO> deleteItem(DeleteItemReqDTO reqDTO) {
        try {
            log.info("开始删除商品: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            // 验证参数
            if (reqDTO.getXianyuAccountId() == null) {
                log.error("账号ID不能为空");
                return ResultObject.failed("账号ID不能为空");
            }
            
            if (reqDTO.getXyGoodsId() == null || reqDTO.getXyGoodsId().isEmpty()) {
                log.error("商品ID不能为空");
                return ResultObject.failed("商品ID不能为空");
            }
            
            // 删除商品信息
            boolean deleted = goodsInfoService.deleteGoodsInfo(
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            DeleteItemRespDTO respDTO = new DeleteItemRespDTO();
            if (deleted) {
                respDTO.setMessage("商品删除成功");
                log.info("商品删除成功: xianyuAccountId={}, xyGoodsId={}", 
                        reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
                return ResultObject.success(respDTO);
            } else {
                respDTO.setMessage("商品删除失败，商品可能不存在");
                log.warn("商品删除失败: xianyuAccountId={}, xyGoodsId={}", 
                        reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
                return ResultObject.failed("商品删除失败");
            }
        } catch (Exception e) {
            log.error("删除商品异常: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), e);
            return ResultObject.failed("删除商品异常: " + e.getMessage());
        }
    }
    
    /**
     * 确保Cookie中包含 _m_h5_tk
     * 如果cookie_text中没有，则从数据库的m_h5_tk字段补充
     */
    private String ensureMh5tkInCookie(String cookieText, Long accountId) {
        try {
            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieText);
            
            // 如果已经包含 _m_h5_tk，直接返回
            if (cookies.containsKey("_m_h5_tk") && !cookies.get("_m_h5_tk").isEmpty()) {
                return cookieText;
            }
            
            // 从数据库获取 m_h5_tk
            String mH5Tk = accountService.getMh5tkByAccountId(accountId);
            if (mH5Tk != null && !mH5Tk.isEmpty()) {
                log.info("从数据库m_h5_tk字段补充token: accountId={}", accountId);
                cookies.put("_m_h5_tk", mH5Tk);
                return XianyuSignUtils.formatCookies(cookies);
            }
            
            log.warn("数据库中也没有m_h5_tk: accountId={}", accountId);
            return cookieText;
            
        } catch (Exception e) {
            log.error("补充m_h5_tk失败: accountId={}", accountId, e);
            return cookieText;
        }
    }
    
    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyConfigMapper autoReplyConfigMapper;
    
    /**
     * 获取RAG自动回复配置
     */
    @Override
    public ResultObject<RagAutoReplyConfigRespDTO> getRagAutoReplyConfig(RagAutoReplyConfigReqDTO reqDTO) {
        try {
            XianyuGoodsAutoReplyConfig config = autoReplyConfigMapper.selectByAccountAndGoodsId(
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            RagAutoReplyConfigRespDTO respDTO = new RagAutoReplyConfigRespDTO();
            if (config != null) {
                respDTO.setRagDelaySeconds(config.getRagDelaySeconds() != null ? config.getRagDelaySeconds() : 15);
            } else {
                // 默认值
                respDTO.setRagDelaySeconds(15);
            }
            
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取RAG自动回复配置失败", e);
            return ResultObject.failed("获取RAG自动回复配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新RAG自动回复配置
     */
    @Override
    public ResultObject<?> updateRagAutoReplyConfig(UpdateRagAutoReplyConfigReqDTO reqDTO) {
        try {
            // 查询现有配置
            XianyuGoodsAutoReplyConfig config = autoReplyConfigMapper.selectByAccountAndGoodsId(
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            if (config == null) {
                // 创建新配置
                config = new XianyuGoodsAutoReplyConfig();
                config.setXianyuAccountId(reqDTO.getXianyuAccountId());
                config.setXyGoodsId(reqDTO.getXyGoodsId());
                config.setRagDelaySeconds(reqDTO.getRagDelaySeconds());
                autoReplyConfigMapper.insert(config);
                log.info("创建RAG自动回复配置: accountId={}, goodsId={}", 
                        reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            } else {
                // 更新现有配置
                config.setRagDelaySeconds(reqDTO.getRagDelaySeconds());
                autoReplyConfigMapper.update(config);
                log.info("更新RAG自动回复配置: accountId={}, goodsId={}", 
                        reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            }
            
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("更新RAG自动回复配置失败", e);
            return ResultObject.failed("更新RAG自动回复配置失败: " + e.getMessage());
        }
    }
}
