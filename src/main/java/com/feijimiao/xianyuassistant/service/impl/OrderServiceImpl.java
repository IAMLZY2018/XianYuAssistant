package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsOrder;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsOrderMapper;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.OrderService;
import com.feijimiao.xianyuassistant.utils.XianyuApiCallUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务实现
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private XianyuApiCallUtils xianyuApiCallUtils;

    @Autowired
    private XianyuGoodsOrderMapper orderMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    @Override
    public String confirmShipment(Long accountId, String orderId) {
        return confirmShipmentToXianyu(accountId, orderId);
    }
    
    @Override
    public String confirmShipmentToXianyu(Long accountId, String orderId) {
        try {
            log.info("【账号{}】开始调用闲鱼API确认发货: orderId={}", accountId, orderId);
            
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到Cookie", accountId);
                return null;
            }
            
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("orderId", orderId);
            dataMap.put("tradeText", "");
            dataMap.put("picList", new String[0]);
            dataMap.put("newUnconsign", true);
            
            log.info("【账号{}】data参数: {}", accountId, dataMap);
            
            XianyuApiCallUtils.ApiCallResult result = xianyuApiCallUtils.callApiWithRetry(
                    accountId, 
                    "mtop.taobao.idle.logistic.consign.dummy", 
                    dataMap, 
                    cookieStr
            );
            
            if (!result.isSuccess()) {
                String errorMsg = result.getErrorMessage();
                log.error("【账号{}】❌ 闲鱼API确认发货失败: {}", accountId, errorMsg);
                
                if (result.isTokenExpired()) {
                    return "令牌过期，请稍后重试或手动更新Cookie";
                }

                if (errorMsg != null && errorMsg.contains("ORDER_ALREADY_DELIVERY")) {
                    log.info("【账号{}】订单已发货(ORDER_ALREADY_DELIVERY)，视为确认成功: orderId={}", accountId, orderId);
                    return "确认发货成功(已发货)";
                }
                
                return null;
            }
            
            Map<String, Object> responseData = result.extractData();
            if (responseData != null) {
                log.info("【账号{}】✅ 闲鱼API确认发货成功: orderId={}", accountId, orderId);
                return "确认发货成功";
            } else {
                log.error("【账号{}】响应数据格式错误", accountId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】调用闲鱼API确认发货异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }

    @Override
    public String getOrderDetail(Long accountId, String orderId) {
        try {
            log.info("【账号{}】开始获取订单详情: orderId={}", accountId, orderId);

            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.error("【账号{}】未找到Cookie", accountId);
                return null;
            }

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("tid", orderId);

            XianyuApiCallUtils.ApiCallResult result = xianyuApiCallUtils.callApiWithRetry(
                    accountId,
                    "mtop.taobao.idle.trade.merchant.full.info",
                    dataMap,
                    cookieStr
            );

            if (!result.isSuccess()) {
                log.warn("【账号{}】获取订单详情失败: orderId={}, error={}", accountId, orderId, result.getErrorMessage());
                return null;
            }

            Map<String, Object> responseData = result.extractData();
            if (responseData == null) {
                log.warn("【账号{}】订单详情响应数据为空: orderId={}", accountId, orderId);
                return null;
            }

            String json = objectMapper.writeValueAsString(responseData);
            log.info("【账号{}】获取订单详情成功: orderId={}", accountId, orderId);

            updateOrderDetailFromApi(accountId, orderId, responseData);

            return json;
        } catch (Exception e) {
            log.error("【账号{}】获取订单详情异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void updateOrderDetailFromApi(Long accountId, String orderId, Map<String, Object> responseData) {
        try {
            XianyuGoodsOrder order = orderMapper.selectByAccountIdAndOrderId(accountId, orderId);
            if (order == null) {
                log.debug("【账号{}】本地无此订单记录，跳过更新: orderId={}", accountId, orderId);
                return;
            }

            Object moduleObj = responseData.get("module");
            if (!(moduleObj instanceof Map)) return;
            Map<String, Object> module = (Map<String, Object>) moduleObj;

            String buyerUserName = null;
            Object merchantBuyerVO = module.get("merchantBuyerVO");
            if (merchantBuyerVO instanceof Map) {
                Map<String, Object> buyer = (Map<String, Object>) merchantBuyerVO;
                Object userNick = buyer.get("userNick");
                if (userNick instanceof String) buyerUserName = (String) userNick;
            }

            String orderCreateTime = null;
            String paySuccessTime = null;
            String consignTime = null;
            Object merchantCommonData = module.get("merchantCommonData");
            if (merchantCommonData instanceof Map) {
                Map<String, Object> commonData = (Map<String, Object>) merchantCommonData;
                Object ct = commonData.get("createTime");
                if (ct instanceof String) orderCreateTime = (String) ct;
                Object pt = commonData.get("paySuccessTime");
                if (pt instanceof String) paySuccessTime = (String) pt;
                Object ct2 = commonData.get("consignTime");
                if (ct2 instanceof String) consignTime = (String) ct2;
            }

            String goodsTitle = null;
            Object merchantItemVO = module.get("merchantItemVO");
            if (merchantItemVO instanceof Map) {
                Map<String, Object> merchantItem = (Map<String, Object>) merchantItemVO;
                Object title = merchantItem.get("title");
                if (title instanceof String) goodsTitle = (String) title;
            }

            String totalPrice = null;
            Integer buyNum = null;
            Object merchantPriceVO = module.get("merchantPriceVO");
            if (merchantPriceVO instanceof Map) {
                Map<String, Object> priceVO = (Map<String, Object>) merchantPriceVO;
                Object tp = priceVO.get("totalPrice");
                if (tp instanceof String) totalPrice = (String) tp;
                Object bn = priceVO.get("buyNum");
                if (bn instanceof String) {
                    try { buyNum = Integer.parseInt((String) bn); } catch (Exception e) { buyNum = 1; }
                } else if (bn instanceof Number) {
                    buyNum = ((Number) bn).intValue();
                }
            }

            orderMapper.updateOrderDetail(order.getId(), buyerUserName, orderCreateTime, paySuccessTime, consignTime, null, goodsTitle, totalPrice, buyNum);
            log.info("【账号{}】从API更新订单详情成功: orderId={}", accountId, orderId);
        } catch (Exception e) {
            log.warn("【账号{}】更新订单详情失败: orderId={}", accountId, orderId, e);
        }
    }

    @Override
    public String getOrderDetailFromLocal(Long accountId, String orderId) {
        try {
            XianyuGoodsOrder order = orderMapper.selectByAccountIdAndOrderId(accountId, orderId);
            if (order == null) {
                log.warn("【账号{}】本地未找到订单: orderId={}", accountId, orderId);
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getOrderId());
            result.put("xyGoodsId", order.getXyGoodsId());
            result.put("goodsTitle", order.getGoodsTitle());
            result.put("buyerUserName", order.getBuyerUserName());
            result.put("content", order.getContent());
            result.put("state", order.getState());
            result.put("failReason", order.getFailReason());
            result.put("confirmState", order.getConfirmState());
            result.put("createTime", order.getCreateTime());
            result.put("skuName", order.getSkuName());
            result.put("orderCreateTime", order.getOrderCreateTime());
            result.put("paySuccessTime", order.getPaySuccessTime());
            result.put("consignTime", order.getConsignTime());
            result.put("totalPrice", order.getTotalPrice());
            result.put("buyNum", order.getBuyNum());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("【账号{}】获取本地订单详情异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }
}
