package com.foxapplication.simplerpc;

import org.smartboot.http.server.WebSocketRequest;
import org.smartboot.http.server.WebSocketResponse;

import java.util.Optional;

/**
 * 表示一个RPC服务器接口，定义了处理WebSocket请求的方法。
 */
public interface RPCServer {

    /**
     * 处理WebSocket请求的方法。
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data     API响应数据
     * @return 一个包含API响应的Optional对象
     */
    Optional<APIResponse> handle(WebSocketRequest request, WebSocketResponse response, APIResponse data);
}
