package com.feijimiao.xianyuassistant.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * 闲鱼设备工具类
 * 参考Python的goofish_js_version_2.js实现
 */
@Slf4j
public class XianyuDeviceUtils {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random random = new Random();

    /**
     * 生成设备ID
     * 参考Python的generate_device_id方法
     * 格式: UUID格式-用户ID
     * 例如: ED4CBA2C-5DA0-4154-A902-BF5CB52409E2-3888777108
     *
     * @param userId 用户ID (unb字段)
     * @return 设备ID
     */
    public static String generateDeviceId(String userId) {
        char[] result = new char[36];
        
        for (int i = 0; i < 36; i++) {
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                result[i] = '-';
            } else if (i == 14) {
                result[i] = '4';
            } else {
                int randomValue = random.nextInt(16);
                if (i == 19) {
                    // 第19位特殊处理: 3 & randomValue | 8
                    result[i] = CHARS.charAt((3 & randomValue) | 8);
                } else {
                    result[i] = CHARS.charAt(randomValue);
                }
            }
        }
        
        return new String(result) + "-" + userId;
    }

    /**
     * 生成消息ID (mid)
     * 参考Python的generate_mid方法
     * 格式: 随机数(0-999) + 时间戳(毫秒) + " 0"
     * 例如: 1231741667630548 0
     *
     * @return 消息ID
     */
    public static String generateMid() {
        int randomPart = random.nextInt(1000);
        long timestamp = System.currentTimeMillis();
        return randomPart + String.valueOf(timestamp) + " 0";
    }

    /**
     * 生成UUID
     * 参考Python的generate_uuid方法
     * 格式: -时间戳1
     * 例如: -17416676305481
     *
     * @return UUID
     */
    public static String generateUuid() {
        long timestamp = System.currentTimeMillis();
        return "-" + timestamp + "1";
    }

    public static void main(String[] args) {
        // 测试
        String userId = "3888777108";
        String deviceId = generateDeviceId(userId);
        System.out.println("设备ID: " + deviceId);
        
        String mid = generateMid();
        System.out.println("消息ID: " + mid);
        
        String uuid = generateUuid();
        System.out.println("UUID: " + uuid);
    }
}
