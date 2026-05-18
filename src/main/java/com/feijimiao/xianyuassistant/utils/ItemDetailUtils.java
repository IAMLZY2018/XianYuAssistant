package com.feijimiao.xianyuassistant.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSku;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSkuProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ItemDetailUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static String extractDescFromDetailJson(String detailJson) {
        if (detailJson == null || detailJson.isEmpty()) {
            return detailJson;
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(detailJson);
            
            JsonNode itemDONode = rootNode.get("itemDO");
            if (itemDONode == null || itemDONode.isNull()) {
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null && !dataNode.isNull()) {
                    itemDONode = dataNode.get("itemDO");
                }
            }
            
            if (itemDONode != null && !itemDONode.isNull()) {
                JsonNode descNode = itemDONode.get("desc");
                if (descNode != null && !descNode.isNull()) {
                    String desc = descNode.asText();
                    log.info("成功提取desc字段，长度: {}", desc.length());
                    return desc;
                } else {
                    log.warn("itemDO中未找到desc字段");
                }
            } else {
                log.warn("未找到itemDO字段");
            }
            
            log.warn("无法提取desc字段，返回原始JSON，长度: {}", detailJson.length());
            return detailJson;
        } catch (Exception e) {
            log.error("解析商品详情JSON失败，返回原始JSON: {}", e.getMessage());
            return detailJson;
        }
    }

    public static List<XianyuGoodsSku> extractSkuList(String detailJson) {
        List<XianyuGoodsSku> result = new ArrayList<>();
        if (detailJson == null || detailJson.isEmpty()) {
            return result;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(detailJson);

            JsonNode itemDONode = rootNode.get("itemDO");
            if (itemDONode == null || itemDONode.isNull()) {
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null && !dataNode.isNull()) {
                    itemDONode = dataNode.get("itemDO");
                }
            }

            if (itemDONode == null || itemDONode.isNull()) {
                log.warn("未找到itemDO字段，无法解析SKU");
                return result;
            }

            JsonNode skuListNode = itemDONode.get("skuList");
            if (skuListNode == null || !skuListNode.isArray()) {
                skuListNode = itemDONode.get("idleItemSkuList");
            }

            if (skuListNode == null || !skuListNode.isArray() || skuListNode.size() == 0) {
                log.info("商品无SKU数据");
                return result;
            }

            for (JsonNode skuNode : skuListNode) {
                XianyuGoodsSku sku = new XianyuGoodsSku();

                if (skuNode.has("skuId") && !skuNode.get("skuId").isNull()) {
                    sku.setSkuId(String.valueOf(skuNode.get("skuId").asLong()));
                }
                if (skuNode.has("price") && !skuNode.get("price").isNull()) {
                    sku.setPrice(skuNode.get("price").asInt());
                }
                if (skuNode.has("quantity") && !skuNode.get("quantity").isNull()) {
                    sku.setQuantity(skuNode.get("quantity").asInt());
                }

                JsonNode propertyListNode = skuNode.get("propertyList");
                if (propertyListNode != null && propertyListNode.isArray() && propertyListNode.size() > 0) {
                    StringBuilder propertyTextBuilder = new StringBuilder();
                    StringBuilder valueTextBuilder = new StringBuilder();
                    for (JsonNode propNode : propertyListNode) {
                        if (propNode.has("propertyText") && !propNode.get("propertyText").isNull()) {
                            String propText = propNode.get("propertyText").asText();
                            String valText = propNode.has("valueText") ? propNode.get("valueText").asText() : "";
                            if (propertyTextBuilder.length() > 0) {
                                propertyTextBuilder.append(" ");
                                valueTextBuilder.append(" ");
                            }
                            propertyTextBuilder.append(propText).append(":").append(valText);
                            valueTextBuilder.append(valText);
                        }
                    }
                    sku.setPropertyText(propertyTextBuilder.toString());
                    sku.setValueText(valueTextBuilder.toString());

                    JsonNode firstProp = propertyListNode.get(0);
                    if (firstProp.has("propertyId") && !firstProp.get("propertyId").isNull()) {
                        sku.setPropertyId(firstProp.get("propertyId").asInt());
                    }
                    if (firstProp.has("valueId") && !firstProp.get("valueId").isNull()) {
                        sku.setValueId(firstProp.get("valueId").asInt());
                    }
                    if (firstProp.has("propertySortOrder") && !firstProp.get("propertySortOrder").isNull()) {
                        sku.setPropertySortOrder(firstProp.get("propertySortOrder").asInt());
                    }
                    if (firstProp.has("valueSortOrder") && !firstProp.get("valueSortOrder").isNull()) {
                        sku.setValueSortOrder(firstProp.get("valueSortOrder").asInt());
                    }
                }

                if (skuNode.has("features") && !skuNode.get("features").isNull()) {
                    sku.setFeatures(skuNode.get("features").toString());
                }

                result.add(sku);
            }

            log.info("解析SKU列表成功，数量: {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("解析商品SKU失败: {}", e.getMessage());
            return result;
        }
    }

    public static List<XianyuGoodsSkuProperty> extractSkuPropertyList(String detailJson) {
        List<XianyuGoodsSkuProperty> result = new ArrayList<>();
        if (detailJson == null || detailJson.isEmpty()) {
            return result;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(detailJson);

            JsonNode itemDONode = rootNode.get("itemDO");
            if (itemDONode == null || itemDONode.isNull()) {
                JsonNode dataNode = rootNode.get("data");
                if (dataNode != null && !dataNode.isNull()) {
                    itemDONode = dataNode.get("itemDO");
                }
            }

            if (itemDONode == null || itemDONode.isNull()) {
                return result;
            }

            JsonNode skuListNode = itemDONode.get("skuList");
            if (skuListNode == null || !skuListNode.isArray()) {
                skuListNode = itemDONode.get("idleItemSkuList");
            }

            if (skuListNode == null || !skuListNode.isArray() || skuListNode.size() == 0) {
                return result;
            }

            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode skuNode : skuListNode) {
                JsonNode propertyListNode = skuNode.get("propertyList");
                if (propertyListNode == null || !propertyListNode.isArray()) continue;
                for (JsonNode propNode : propertyListNode) {
                    if (!propNode.has("propertyId") || !propNode.has("valueId")) continue;
                    int propertyId = propNode.get("propertyId").asInt();
                    int valueId = propNode.get("valueId").asInt();
                    String key = propertyId + ":" + valueId;
                    if (seen.contains(key)) continue;
                    seen.add(key);

                    XianyuGoodsSkuProperty prop = new XianyuGoodsSkuProperty();
                    prop.setPropertyId(propertyId);
                    prop.setPropertyText(propNode.has("propertyText") ? propNode.get("propertyText").asText() : "");
                    prop.setPropertySortOrder(propNode.has("propertySortOrder") ? propNode.get("propertySortOrder").asInt() : 0);
                    prop.setValueId(valueId);
                    prop.setValueText(propNode.has("valueText") ? propNode.get("valueText").asText() : "");
                    prop.setValueSortOrder(propNode.has("valueSortOrder") ? propNode.get("valueSortOrder").asInt() : 0);
                    result.add(prop);
                }
            }

            log.info("解析SKU属性维度成功，数量: {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("解析SKU属性维度失败: {}", e.getMessage());
            return result;
        }
    }
}
