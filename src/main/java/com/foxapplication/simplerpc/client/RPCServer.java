package com.foxapplication.simplerpc.client;

import com.foxapplication.simplerpc.common.APIResponse;

import java.util.Optional;

/**
 * 表示一个RPC服务器接口，定义了处理WebSocket请求的方法。
 */
public interface RPCServer {

    /**
     * 处理WebSocket请求的方法。
     *
     * @param data     API响应数据
     * @return 一个包含API响应的Optional对象
     */
    Optional<APIResponse> handle(APIResponse data);
}
