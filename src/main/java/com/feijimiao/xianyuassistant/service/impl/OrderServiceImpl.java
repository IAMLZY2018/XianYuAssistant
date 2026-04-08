package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.controller.dto.OrderQueryReqDTO;
import com.feijimiao.xianyuassistant.controller.vo.OrderVO;
import com.feijimiao.xianyuassistant.entity.XianyuOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuOrderMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.utils.XianyuApiUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private XianyuOrderMapper orderMapper;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 确认发货API URL
     */
    private static final String CONFIRM_SHIPMENT_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idle.logistic.consign.dummy/1.0/";
    
    @Override
    public Long saveOrUpdateOrder(XianyuOrder order) {
        try {
            // 查询是否已存在
            XianyuOrder existingOrder = getOrderByAccountIdAndOrderId(
                    order.getXianyuAccountId(), 
                    order.getOrderId()
            );
            
            if (existingOrder != null) {
                // 更新已存在的订单
                order.setId(existingOrder.getId());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("更新订单成功: orderId={}, accountId={}", 
                        order.getOrderId(), order.getXianyuAccountId());
                return existingOrder.getId();
            } else {
                // 插入新订单
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.insert(order);
                log.info("保存新订单成功: orderId={}, accountId={}, id={}", 
                        order.getOrderId(), order.getXianyuAccountId(), order.getId());
                return order.getId();
            }
            
        } catch (Exception e) {
            log.error("保存或更新订单失败: orderId={}, accountId={}", 
                    order.getOrderId(), order.getXianyuAccountId(), e);
            return null;
        }
    }
    
    @Override
    public XianyuOrder getOrderByOrderId(String orderId) {
        try {
            return orderMapper.selectOne(
                    new LambdaQueryWrapper<XianyuOrder>()
                            .eq(XianyuOrder::getOrderId, orderId)
            );
        } catch (Exception e) {
            log.error("查询订单失败: orderId={}", orderId, e);
            return null;
        }
    }
    
    @Override
    public XianyuOrder getOrderByAccountIdAndOrderId(Long accountId, String orderId) {
        try {
            return orderMapper.selectOne(
                    new LambdaQueryWrapper<XianyuOrder>()
                            .eq(XianyuOrder::getXianyuAccountId, accountId)
                            .eq(XianyuOrder::getOrderId, orderId)
            );
        } catch (Exception e) {
            log.error("查询订单失败: accountId={}, orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    @Override
    public String confirmShipment(Long accountId, String orderId) {
        // 调用闲鱼API确认发货
        return confirmShipmentToXianyu(accountId, orderId);
    }
    
    @Override
    public String confirmShipmentToXianyu(Long accountId, String orderId) {
        try {
            log.info("【账号{}】开始调用闲鱼API确认发货: orderId={}", accountId, orderId);
            
            // 获取Cookie
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到Cookie", accountId);
                return null;
            }
            
            // 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            
            // 提取token
            String token = XianyuSignUtils.extractToken(cookies);
            if (token.isEmpty()) {
                log.error("【账号{}】Cookie中缺少_m_h5_tk字段", accountId);
                return null;
            }
            
            // 生成时间戳
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // 构造data参数（参考Python代码）
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderId", orderId);
            dataMap.put("tradeText", "");
            dataMap.put("picList", new String[0]);
            dataMap.put("newUnconsign", true);
            String dataVal = objectMapper.writeValueAsString(dataMap);
            
            log.info("【账号{}】data参数: {}", accountId, dataVal);
            
            // 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);
            
            log.info("【账号{}】签名生成: timestamp={}, token={}, sign={}", 
                    accountId, timestamp, token.substring(0, Math.min(10, token.length())) + "...", sign);
            
            // 构造URL参数
            Map<String, String> params = new HashMap<>();
            params.put("jsv", "2.7.2");
            params.put("appKey", "34839810");
            params.put("t", timestamp);
            params.put("sign", sign);
            params.put("v", "1.0");
            params.put("type", "originaljson");
            params.put("accountSite", "xianyu");
            params.put("dataType", "json");
            params.put("timeout", "20000");
            params.put("api", "mtop.taobao.idle.logistic.consign.dummy");
            params.put("sessionOption", "AutoLoginOnly");
            
            // 构造完整URL
            StringBuilder urlBuilder = new StringBuilder(CONFIRM_SHIPMENT_URL);
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                        .append("&");
            }
            String url = urlBuilder.substring(0, urlBuilder.length() - 1);
            
            log.info("【账号{}】请求URL: {}", accountId, url);
            
            // 构造POST body
            String postBody = "data=" + URLEncoder.encode(dataVal, StandardCharsets.UTF_8);
            
            // 构造请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Cookie", cookieStr)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .header("Referer", "https://market.m.goofish.com/")
                    .header("Origin", "https://market.m.goofish.com")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .POST(HttpRequest.BodyPublishers.ofString(postBody))
                    .timeout(Duration.ofSeconds(20))
                    .build();
            
            // 发送请求
            log.info("【账号{}】发送确认发货请求...", accountId);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("【账号{}】响应状态码: {}", accountId, response.statusCode());
            log.info("【账号{}】响应内容: {}", accountId, response.body());
            
            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            
            // 检查响应
            if (result.containsKey("ret")) {
                @SuppressWarnings("unchecked")
                List<String> ret = (List<String>) result.get("ret");
                if (ret != null && !ret.isEmpty()) {
                    String retCode = ret.get(0);
                    
                    // 成功情况
                    if (retCode.contains("SUCCESS")) {
                        log.info("【账号{}】✅ 闲鱼API确认发货成功: orderId={}", accountId, orderId);
                        // 更新本地数据库
                        return updateOrderStatusToShipped(accountId, orderId, "确认发货成功");
                    }
                    
                    // 已经发货的情况，也视为成功
                    if (retCode.contains("ORDER_ALREADY_DELIVERY")) {
                        log.info("【账号{}】✅ 订单已经发货成功: orderId={}", accountId, orderId);
                        // 更新本地数据库
                        return updateOrderStatusToShipped(accountId, orderId, "订单已经发货成功");
                    }
                }
            }
            
            log.error("【账号{}】❌ 闲鱼API确认发货失败: {}", accountId, result);
            return null;
            
        } catch (Exception e) {
            log.error("【账号{}】调用闲鱼API确认发货异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    /**
     * 更新订单状态为已发货
     * 同时同步自动发货记录的状态
     */
    private String updateOrderStatusToShipped(Long accountId, String orderId, String successMessage) {
        try {
            // 查询订单
            XianyuOrder order = getOrderByAccountIdAndOrderId(accountId, orderId);
            if (order == null) {
                log.warn("订单不存在: accountId={}, orderId={}", accountId, orderId);
                return null;
            }
            
            // 更新订单状态为已发货
            order.setOrderStatus(3);
            order.setOrderStatusText("已发货");
            order.setOrderDeliveryTime(System.currentTimeMillis());
            order.setUpdateTime(LocalDateTime.now());
            
            int result = orderMapper.updateById(order);
            if (result > 0) {
                log.info("【账号{}】更新订单状态成功: orderId={}, orderStatus=3(已发货)", accountId, orderId);
                
                // 同步自动发货记录的状态
                syncAutoDeliveryRecordStatus(accountId, orderId);
                
                return successMessage;
            } else {
                log.error("【账号{}】更新订单状态失败: orderId={}", accountId, orderId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】更新订单状态异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
    
    /**
     * 同步自动发货记录的状态
     * 当订单状态更新为"已发货"(order_status=3)时,自动发货记录的状态也应该同步
     * 
     * @param accountId 账号ID
     * @param orderId 订单ID
     */
    private void syncAutoDeliveryRecordStatus(Long accountId, String orderId) {
        try {
            // 查询自动发货记录
            com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord record = 
                autoDeliveryRecordMapper.selectByOrderId(accountId, orderId);
            
            if (record != null) {
                log.info("【账号{}】找到自动发货记录: recordId={}, orderId={}, 当前state={}", 
                        accountId, record.getId(), orderId, record.getState());
                
                // 如果自动发货记录存在,且发货成功(state=1),则记录日志
                // 订单状态已通过xianyu_order.order_status=3表示已确认发货
                if (record.getState() == 1) {
                    log.info("【账号{}】✅ 订单已确认发货: orderId={}, 自动发货记录ID={}, 发货状态=成功", 
                            accountId, orderId, record.getId());
                } else {
                    log.warn("【账号{}】⚠️ 订单已确认发货,但自动发货记录显示失败: orderId={}, recordId={}, state={}", 
                            accountId, orderId, record.getId(), record.getState());
                }
            } else {
                log.debug("【账号{}】未找到对应的自动发货记录: orderId={}", accountId, orderId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】同步自动发货记录状态失败: orderId={}", accountId, orderId, e);
        }
    }
    
    @Override
    public Page<OrderVO> queryOrderList(OrderQueryReqDTO reqDTO) {
        try {
            // 创建分页对象
            Page<OrderVO> page = new Page<>(reqDTO.getPageNum(), reqDTO.getPageSize());
            
            // 调用Mapper联表查询
            Page<OrderVO> result = orderMapper.queryOrderList(
                    page,
                    reqDTO.getXianyuAccountId(),
                    reqDTO.getXyGoodsId(),
                    reqDTO.getOrderStatus()
            );
            
            log.info("查询订单列表成功: total={}, current={}, size={}", 
                    result.getTotal(), result.getCurrent(), result.getSize());
            
            return result;
            
        } catch (Exception e) {
            log.error("查询订单列表失败", e);
            return new Page<>();
        }
    }
}
