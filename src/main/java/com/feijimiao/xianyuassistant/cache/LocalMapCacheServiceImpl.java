package com.feijimiao.xianyuassistant.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于ConcurrentHashMap的本地缓存实现
 * 后续可新增RedisCacheServiceImpl替换此实现
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@Service
public class LocalMapCacheServiceImpl implements CacheService {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value) {
        cache.put(key, new CacheEntry(value, -1));
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
        cache.put(key, new CacheEntry(value, expireAt));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    @Override
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public long increment(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            // 不存在或已过期，从1开始
            AtomicLong counter = new AtomicLong(1);
            cache.put(key, new CacheEntry(counter, -1));
            return 1;
        }
        Object value = entry.getValue();
        if (value instanceof AtomicLong) {
            return ((AtomicLong) value).incrementAndGet();
        }
        // 类型不匹配，重新初始化
        AtomicLong counter = new AtomicLong(1);
        cache.put(key, new CacheEntry(counter, -1));
        return 1;
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            long expireAt = System.currentTimeMillis() + unit.toMillis(timeout);
            cache.put(key, new CacheEntry(entry.getValue(), expireAt));
        }
    }

    @Override
    public long getExpire(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return -2;
        }
        if (entry.getExpireAt() == -1) {
            return -1;
        }
        long remaining = entry.getExpireAt() - System.currentTimeMillis();
        if (remaining <= 0) {
            cache.remove(key);
            return -2;
        }
        return remaining;
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long expireAt; // -1表示永不过期

        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        Object getValue() {
            return value;
        }

        long getExpireAt() {
            return expireAt;
        }

        boolean isExpired() {
            return expireAt != -1 && System.currentTimeMillis() > expireAt;
        }
    }
}
