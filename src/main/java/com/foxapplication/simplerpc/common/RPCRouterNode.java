package com.foxapplication.simplerpc.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.hutool.core.text.StrUtil;

import java.util.concurrent.ConcurrentHashMap;

@Getter // 自动生成所有字段的getter方法
@AllArgsConstructor // 自动生成包含所有字段的构造函数
@NoArgsConstructor // 自动生成无参构造函数
public class RPCRouterNode<T> {

    /**
     * 用于存储子节点的并发哈希映射。
     */
    private final ConcurrentHashMap<String, RPCRouterNode<T>> children = new ConcurrentHashMap<>();

    /**
     * 节点的名称，默认值为空字符串。
     */
    @Setter // 自动生成name字段的setter方法
    @Getter // 自动生成name字段的getter方法
    private String name = StrUtil.EMPTY;

    /**
     * 关联的RPC服务器实例，默认值为null。
     */
    @Setter // 自动生成rpcServer字段的setter方法
    @Getter // 自动生成rpcServer字段的getter方法
    private T rpcServer = null;

    /**
     * 清空所有子节点。
     */
    public void clear() {
        children.clear();
    }

    /**
     * 静态工厂方法，创建一个新的RPCRouterNode实例。
     *
     * @param key 新节点的名称
     * @return 一个带有指定名称的新RPCRouterNode实例
     */
    public static <T> RPCRouterNode<T> create(String key) {
        return new RPCRouterNode<T>(key, null);
    }
}
