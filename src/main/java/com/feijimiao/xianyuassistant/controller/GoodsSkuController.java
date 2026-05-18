package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSku;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsSkuProperty;
import com.feijimiao.xianyuassistant.service.GoodsSkuService;
import com.feijimiao.xianyuassistant.service.GoodsSkuPropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/goods-sku")
@CrossOrigin(origins = "*")
public class GoodsSkuController {

    @Autowired
    private GoodsSkuService goodsSkuService;

    @Autowired
    private GoodsSkuPropertyService goodsSkuPropertyService;

    @PostMapping("/list")
    public ResultObject<List<XianyuGoodsSku>> listByGoodsId(@RequestParam("xyGoodsId") String xyGoodsId) {
        try {
            List<XianyuGoodsSku> skuList = goodsSkuService.listByXyGoodsId(xyGoodsId);
            return ResultObject.success(skuList);
        } catch (Exception e) {
            log.error("查询商品SKU列表失败: xyGoodsId={}", xyGoodsId, e);
            return ResultObject.failed("查询商品SKU列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/detail")
    public ResultObject<Map<String, Object>> skuDetail(@RequestParam("xyGoodsId") String xyGoodsId) {
        try {
            List<XianyuGoodsSku> skuList = goodsSkuService.listByXyGoodsId(xyGoodsId);
            List<XianyuGoodsSkuProperty> propertyList = goodsSkuPropertyService.listByXyGoodsId(xyGoodsId);
            Map<String, Object> data = new HashMap<>();
            data.put("skuList", skuList);
            data.put("propertyList", propertyList);
            return ResultObject.success(data);
        } catch (Exception e) {
            log.error("查询商品SKU详情失败: xyGoodsId={}", xyGoodsId, e);
            return ResultObject.failed("查询商品SKU详情失败: " + e.getMessage());
        }
    }
}
