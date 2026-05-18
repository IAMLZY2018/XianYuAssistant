package com.feijimiao.xianyuassistant.service.delivery;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsSku;
import com.feijimiao.xianyuassistant.service.GoodsSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SKU文本解析与匹配器
 *
 * <p>负责两部分工作：</p>
 * <ol>
 *   <li>将API返回的skuInfo字符串（如"颜色:红色;尺码:XL"）解析为可读文本（如"红色 XL"）</li>
 *   <li>将解析后的文本与数据库中的SKU valueText匹配，找到对应的skuId</li>
 * </ol>
 */
@Slf4j
@Component
public class SkuResolver {

    @Autowired
    private GoodsSkuService goodsSkuService;

    /**
     * 将skuInfo字符串解析为值文本
     *
     * <p>输入示例: "颜色:红色;尺码:XL" → 输出: "红色 XL"</p>
     * <p>输入示例: "新号 qq" → 输出: "新号 qq"</p>
     *
     * @param skuInfoStr API返回的skuInfo字符串
     * @return 解析后的值文本
     */
    public String parseSkuInfoToValueText(String skuInfoStr) {
        if (skuInfoStr == null || skuInfoStr.isEmpty()) {
            return null;
        }
        StringBuilder valueText = new StringBuilder();
        String[] props = skuInfoStr.split(";");
        for (String prop : props) {
            String[] kv = prop.split(":");
            if (kv.length >= 2) {
                if (valueText.length() > 0) valueText.append(" ");
                valueText.append(kv[1].trim());
            } else if (kv.length == 1 && !kv[0].trim().isEmpty()) {
                if (valueText.length() > 0) valueText.append(" ");
                valueText.append(kv[0].trim());
            }
        }
        return valueText.toString();
    }

    /**
     * 通过SKU文本匹配数据库中的skuId
     *
     * <p>将输入文本标准化后与数据库中每个SKU的valueText标准化比较。</p>
     * <p>标准化规则：将 "/" 和 ";" 替换为空格，合并连续空格。</p>
     *
     * @param accountId    闲鱼账号ID
     * @param xyGoodsId    闲鱼商品ID
     * @param skuValueText SKU值文本
     * @return 匹配的skuId，null表示未匹配
     */
    public String resolveSkuIdByText(Long accountId, String xyGoodsId, String skuValueText) {
        if (xyGoodsId == null || skuValueText == null || skuValueText.isEmpty()) {
            return null;
        }
        try {
            List<XianyuGoodsSku> skus = goodsSkuService.listByXyGoodsId(xyGoodsId);
            if (skus == null || skus.isEmpty()) {
                log.info("【账号{}】商品无SKU数据: xyGoodsId={}", accountId, xyGoodsId);
                return null;
            }

            String normalizedInput = normalize(skuValueText);
            log.info("【账号{}】SKU匹配: 输入={}, 标准化={}", accountId, skuValueText, normalizedInput);

            for (XianyuGoodsSku sku : skus) {
                String dbValueText = sku.getValueText();
                if (dbValueText == null || dbValueText.isEmpty()) continue;
                if (normalizedInput.equals(normalize(dbValueText))) {
                    log.info("【账号{}】SKU文本匹配成功: input={}, dbValueText={}, skuId={}", accountId, skuValueText, dbValueText, sku.getSkuId());
                    return sku.getSkuId();
                }
            }

            log.info("【账号{}】SKU文本未匹配到skuId: xyGoodsId={}, valueText={}", accountId, xyGoodsId, skuValueText);
            return null;
        } catch (Exception e) {
            log.warn("【账号{}】解析SKU ID异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return null;
        }
    }

    /** 标准化SKU文本：统一分隔符，合并空格 */
    private String normalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replace("/", " ").replace(";", " ").replaceAll("\\s+", " ").trim();
    }
}
