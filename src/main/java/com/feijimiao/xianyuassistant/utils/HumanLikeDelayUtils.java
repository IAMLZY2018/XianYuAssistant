package com.feijimiao.xianyuassistant.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * 模拟人工操作延迟工具类
 * 用于在自动化操作中添加随机延迟，使行为更像真人操作
 */
@Slf4j
public class HumanLikeDelayUtils {
    
    private static final Random random = new Random();
    
    /**
     * 短延迟：500ms - 1500ms
     * 适用于：点击、输入等快速操作
     */
    public static void shortDelay() {
        delay(500, 1500);
    }
    
    /**
     * 中等延迟：1000ms - 3000ms
     * 适用于：页面加载、消息发送等
     */
    public static void mediumDelay() {
        delay(1000, 3000);
    }
    
    /**
     * 长延迟：2000ms - 5000ms
     * 适用于：页面切换、复杂操作等
     */
    public static void longDelay() {
        delay(2000, 5000);
    }
    
    /**
     * 自定义范围延迟
     *
     * @param minMs 最小延迟毫秒数
     * @param maxMs 最大延迟毫秒数
     */
    public static void delay(int minMs, int maxMs) {
        if (minMs < 0 || maxMs < minMs) {
            log.warn("延迟参数无效: minMs={}, maxMs={}", minMs, maxMs);
            return;
        }

        int delayMs = minMs + random.nextInt(maxMs - minMs + 1);

        try {
            log.info("⏱️ 模拟人工操作延迟: {}秒 ({}毫秒)", String.format("%.1f", delayMs / 1000.0), delayMs);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            log.warn("延迟被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 阅读延迟：根据文本长度计算延迟
     * 模拟人阅读文本的时间
     * 
     * @param textLength 文本长度
     * @return 延迟毫秒数
     */
    public static void readingDelay(int textLength) {
        // 假设每个字符阅读时间 50-100ms
        int baseDelay = textLength * 50;
        int randomDelay = random.nextInt(textLength * 50 + 1);
        int totalDelay = Math.min(baseDelay + randomDelay, 5000); // 最多5秒
        
        try {
            log.debug("模拟阅读延迟: {}ms (文本长度: {})", totalDelay, textLength);
            Thread.sleep(totalDelay);
        } catch (InterruptedException e) {
            log.warn("延迟被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 思考延迟：模拟人思考的时间
     * 1000ms - 4000ms
     */
    public static void thinkingDelay() {
        delay(1000, 4000);
    }
    
    /**
     * 打字延迟：根据文本长度模拟打字时间
     * 
     * @param textLength 文本长度
     */
    public static void typingDelay(int textLength) {
        // 假设每个字符打字时间 100-200ms
        int baseDelay = textLength * 100;
        int randomDelay = random.nextInt(textLength * 100 + 1);
        int totalDelay = Math.min(baseDelay + randomDelay, 10000); // 最多10秒
        
        try {
            log.debug("模拟打字延迟: {}ms (文本长度: {})", totalDelay, textLength);
            Thread.sleep(totalDelay);
        } catch (InterruptedException e) {
            log.warn("延迟被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 页面翻页延迟：模拟翻页操作
     * 800ms - 2000ms
     */
    public static void pageScrollDelay() {
        delay(800, 2000);
    }
}
