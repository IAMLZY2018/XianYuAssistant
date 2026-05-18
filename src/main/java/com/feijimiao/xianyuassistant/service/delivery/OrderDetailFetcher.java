package com.feijimiao.xianyuassistant.service.delivery;

import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.GoodsSkuService;
import com.feijimiao.xianyuassistant.utils.XianyuApiCallUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单详情获取器
 *
 * <p>调用闲鱼订单详情API（mtop.taobao.idle.trade.merchant.full.info），
 * 解析买家昵称、下单时间、付款时间、发货时间、商品标题、总价、购买数量、SKU信息。</p>
 */
@Slf4j
@Component
public class OrderDetailFetcher {

    @Autowired
    private AccountService accountService;

    @Autowired
    private XianyuApiCallUtils xianyuApiCallUtils;

    @Autowired
    private SkuResolver skuResolver;

    /**
     * 订单详情信息
     */
    public static class OrderDetailInfo {
        public String skuId;
        public String skuName;
        public String buyerUserName;
        public String orderCreateTime;
        public String paySuccessTime;
        public String consignTime;
        public String goodsTitle;
        public String totalPrice;
        public Integer buyNum;
    }

    /**
     * 获取订单详情
     *
     * @param accountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @param orderId   订单ID
     * @return 订单详情，null表示获取失败
     */
    public OrderDetailInfo fetch(Long accountId, String xyGoodsId, String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }
        OrderDetailInfo info = new OrderDetailInfo();
        try {
            String cookieStr = accountService.getCookieByAccountId(accountId);
            if (cookieStr == null || cookieStr.isEmpty()) {
                log.warn("【账号{}】未找到Cookie，无法获取订单详情", accountId);
                return null;
            }

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("tid", orderId);

            XianyuApiCallUtils.ApiCallResult result = xianyuApiCallUtils.callApiWithRetry(
                    accountId, "mtop.taobao.idle.trade.merchant.full.info", dataMap, cookieStr);

            if (!result.isSuccess()) {
                log.warn("【账号{}】获取订单详情失败: orderId={}, error={}", accountId, orderId, result.getErrorMessage());
                return null;
            }

            Map<String, Object> responseData = result.extractData();
            if (responseData == null) {
                log.warn("【账号{}】订单详情响应数据为空: orderId={}", accountId, orderId);
                return null;
            }

            Object moduleObj = responseData.get("module");
            if (!(moduleObj instanceof Map)) {
                log.warn("【账号{}】订单详情module为空: orderId={}", accountId, orderId);
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> module = (Map<String, Object>) moduleObj;

            parseBuyerInfo(module, info);
            parseCommonData(module, info);
            parseItemInfo(module, info);
            parsePriceInfo(module, info);
            parseSkuInfo(accountId, xyGoodsId, module, info);

            log.info("【账号{}】订单详情解析完成: orderId={}, skuId={}, buyerUserName={}, goodsTitle={}, totalPrice={}, buyNum={}",
                    accountId, orderId, info.skuId, info.buyerUserName, info.goodsTitle, info.totalPrice, info.buyNum);
            return info;
        } catch (Exception e) {
            log.warn("【账号{}】获取订单详情异常: orderId={}", accountId, orderId, e);
            return null;
        }
    }

    private void parseBuyerInfo(Map<String, Object> module, OrderDetailInfo info) {
        Object merchantBuyerVO = module.get("merchantBuyerVO");
        if (merchantBuyerVO instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> buyer = (Map<String, Object>) merchantBuyerVO;
            Object userNick = buyer.get("userNick");
            if (userNick instanceof String) {
                info.buyerUserName = (String) userNick;
            }
        }
    }

    private void parseCommonData(Map<String, Object> module, OrderDetailInfo info) {
        Object merchantCommonData = module.get("merchantCommonData");
        if (merchantCommonData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> commonData = (Map<String, Object>) merchantCommonData;
            Object createTime = commonData.get("createTime");
            if (createTime instanceof String) info.orderCreateTime = (String) createTime;
            Object paySuccessTime = commonData.get("paySuccessTime");
            if (paySuccessTime instanceof String) info.paySuccessTime = (String) paySuccessTime;
            Object consignTime = commonData.get("consignTime");
            if (consignTime instanceof String) info.consignTime = (String) consignTime;
        }
    }

    private void parseItemInfo(Map<String, Object> module, OrderDetailInfo info) {
        Object merchantItemVO = module.get("merchantItemVO");
        if (merchantItemVO instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> merchantItem = (Map<String, Object>) merchantItemVO;
            Object title = merchantItem.get("title");
            if (title instanceof String) info.goodsTitle = (String) title;
        }
    }

    private void parsePriceInfo(Map<String, Object> module, OrderDetailInfo info) {
        Object merchantPriceVO = module.get("merchantPriceVO");
        if (merchantPriceVO instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> priceVO = (Map<String, Object>) merchantPriceVO;
            Object totalPrice = priceVO.get("totalPrice");
            if (totalPrice instanceof String) info.totalPrice = (String) totalPrice;
            Object buyNum = priceVO.get("buyNum");
            if (buyNum instanceof String) {
                try {
                    info.buyNum = Integer.parseInt((String) buyNum);
                } catch (NumberFormatException e) {
                    info.buyNum = 1;
                }
            } else if (buyNum instanceof Number) {
                info.buyNum = ((Number) buyNum).intValue();
            }
        }
    }

    private void parseSkuInfo(Long accountId, String xyGoodsId, Map<String, Object> module, OrderDetailInfo info) {
        // 优先从orderInfoVO解析SKU
        Object orderInfoVO = module.get("orderInfoVO");
        if (orderInfoVO instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderInfo = (Map<String, Object>) orderInfoVO;
            Object itemInfoObj = orderInfo.get("itemInfo");
            if (itemInfoObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemInfo = (Map<String, Object>) itemInfoObj;
                Object skuInfo = itemInfo.get("skuInfo");
                if (skuInfo instanceof String && !((String) skuInfo).isEmpty()) {
                    String skuInfoStr = (String) skuInfo;
                    log.info("【账号{}】从订单详情获取skuInfo: skuInfo={}", accountId, skuInfoStr);
                    String skuValueText = skuResolver.parseSkuInfoToValueText(skuInfoStr);
                    if (skuValueText != null && !skuValueText.isEmpty()) {
                        info.skuName = skuValueText;
                        String skuId = skuResolver.resolveSkuIdByText(accountId, xyGoodsId, skuValueText);
                        if (skuId != null) info.skuId = skuId;
                    }
                }
            }
        }

        // 兜底从merchantItemVO解析规格
        if (info.skuId == null) {
            Object merchantItemVO = module.get("merchantItemVO");
            if (merchantItemVO instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> merchantItem = (Map<String, Object>) merchantItemVO;
                Object itemInfoLinesObj = merchantItem.get("itemInfoLines");
                if (itemInfoLinesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> lines = (List<Map<String, Object>>) itemInfoLinesObj;
                    for (Map<String, Object> line : lines) {
                        Object key = line.get("key");
                        Object value = line.get("value");
                        if ("规格".equals(key) && value instanceof String) {
                            log.info("【账号{}】从merchantItemVO获取规格: value={}", accountId, value);
                            info.skuName = (String) value;
                            String skuId = skuResolver.resolveSkuIdByText(accountId, xyGoodsId, (String) value);
                            if (skuId != null) {
                                info.skuId = skuId;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
