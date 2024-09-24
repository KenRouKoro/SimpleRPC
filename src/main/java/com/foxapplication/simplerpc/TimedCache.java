package com.foxapplication.simplerpc;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
/**
 * 一个具有超时功能的缓存类，当缓存项过期时会触发特定的消费函数。
 *
 * @param <K> 缓存键的类型
 * @param <V> 缓存值的类型
 */
@NoArgsConstructor
public class TimedCache<K, V> {

    /**
     * 缓存的实现，使用ConcurrentHashMap作为底层存储。
     */
    @Getter
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();

    /**
     * 用于执行定时清理的调度器。
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 缓存项的超时时间，单位为毫秒.
     */
    @Setter
    @Getter
    private long timeout = 1000;

    /**
     * 构造一个具有指定超时时间的时间缓存，并启动清理任务.
     */
    @Setter
    @Getter
    private Consumer<Map.Entry<K, V>> onExpire = null;
    @Getter
    @Setter
    private CacheCallback<K,V> callback = null;


    /**
     * 构造一个具有指定超时时间和过期处理逻辑的时间缓存，并启动清理任务.
     * 当缓存项过期时，会由指定的Consumer接口实例处理.
     *
     * @param timeout 缓存项的超时时间，单位为毫秒.
     * @param onExpire 在缓存项过期时执行的处理逻辑.
     */
    public TimedCache(long timeout, Consumer<Map.Entry<K, V>> onExpire) {
        this.timeout = timeout;
        this.onExpire = onExpire;
    }

    /**
     * 向缓存中添加或更新一个键值对。
     * 如果键已存在，则其对应的值将被更新，并且其过期时间将被重置。
     *
     * @param key 键，用于标识缓存中的项。
     * @param value 值，要存储的对象。
     */
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    /**
     * 根据键获取缓存中的值。
     * 如果键不存在或对应的项已过期，则返回null，并从缓存中移除该键。
     *
     * @param key 键，用于查找缓存中的项。
     * @return 缓存中键对应的值，如果键不存在或项已过期，则返回null。
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null || isExpired(entry)) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }

    /**
     * 从缓存中移除指定键的项。
     *
     * @param key 键，要从缓存中移除的项的标识。
     */
    public void remove(K key) {
        cache.remove(key);
    }

    /**
     * 检查给定的缓存项是否已过期。
     * 通过比较当前时间和缓存项的时间戳来判断。
     *
     * @param entry 缓存项，包含值和时间戳。
     * @return 如果缓存项已过期，则返回true；否则返回false。
     */
    private boolean isExpired(CacheEntry<V> entry) {
        return System.currentTimeMillis() - entry.timestamp > timeout;
    }

    /**
     * 启动定时清理任务，定期检查并移除过期的缓存项。
     * 清理任务会定期运行，以确保及时清理过期的缓存项。
     */
    public void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                if (now - entry.getValue().timestamp > timeout) {
                    if (callback!=null){
                        callback.onExpire(entry.getKey(),entry.getValue().value);
                    }
                    cache.remove(entry.getKey());
                    if (onExpire!=null) {
                        onExpire.accept(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().value));
                    }
                }
            }
        }, timeout, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭缓存，包括取消所有的清理任务。
     * 调用此方法可以安全地关闭缓存，确保没有后台任务在运行。
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * 缓存项的内部类。
     * 用于存储缓存项的值和创建时间（时间戳）。
     *
     * @param <V> 缓存项的值的类型。
     */
    public static class CacheEntry<V> {
        private final V value;
        private final long timestamp;

        /**
         * CacheEntry的构造函数。
         * 初始化缓存项的值和时间戳。
         *
         * @param value 缓存项的值。
         * @param timestamp 缓存项的创建时间。
         */
        CacheEntry(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
    public interface CacheCallback<T,V>{
        void onExpire(T key,V value);
    }
}
