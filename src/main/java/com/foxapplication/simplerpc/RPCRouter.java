package com.foxapplication.simplerpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.core.thread.ExecutorBuilder;
import org.smartboot.http.server.WebSocketRequest;
import org.smartboot.http.server.WebSocketResponse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
public class RPCRouter {
    /**
     *  缓存请求，超时会传递错误
     */
    private final TimedCache<String,RPCServer> taskCache = new TimedCache<>();
    /**
     *  SimpleRPC实例
     */
    private final SimpleRPC simpleRPC;
    /**
     *  任务执行器
     */
    private final ExecutorService executorService = ExecutorBuilder.of()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setKeepAliveTime(0)
            .build();
    /**
     *  超时时间
     */
    @Setter
    @Getter
    private long timeout = 60 * 2 * 1000;
    /**
     *  根路由节点
     */
    private final RPCRouterNode root = new RPCRouterNode();

    /**
     * 构造函数，初始化RPC路由器
     *
     * @param simpleRPC SimpleRPC的实例，用于处理RPC请求
     */
    public RPCRouter(SimpleRPC simpleRPC) {
        this.simpleRPC = simpleRPC;

        // 设置根路由的RPC服务器处理逻辑
        root.setRpcServer((request,response,data)->{
            // 从任务缓存中获取与请求UUID关联的RPC服务器实例
            RPCServer server = taskCache.get(data.getUUID());
            if (server != null){
                // 如果找到匹配的RPC服务器实例，则从缓存中移除，以避免重复处理
                taskCache.remove(data.getUUID());
                // 调用找到的RPC服务器实例处理请求并返回处理结果
                return server.handle(request,response,data);
            }
            // 如果没有找到匹配的RPC服务器实例，返回空的Optional
            return Optional.empty();
        });
        taskCache.setCallback((key, value)->{
            APIResponse response = APIResponse.create().UUID(key);
            response.setStatus(408);
            response.setMessage("Request timeout");
            value.handle(null,null,response);
        });
    }

    /**
     * 启动任务
     */
    public void start(){
        // 设置任务缓存的超时时间
        taskCache.setTimeout(timeout);
        // 启动任务清理任务
        taskCache.startCleanupTask();
    }

    /**
     * 处理WebSocket请求的方法，通过字符串数据创建API响应对象
     * @param request WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data 字符串形式的API响应数据
     */
    public void handle(WebSocketRequest request, WebSocketResponse response, String data){
        APIResponse apiResponse;
        try {
            apiResponse = APIResponse.fromStr(data);
        } catch (JsonProcessingException e) {
            log.error("Data conversion failed.",e);
            return;
        }
        handle(request,response,apiResponse);
    }

    /**
     * 处理WebSocket请求的方法，通过二进制数据创建API响应对象
     * @param request WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data 二进制形式的API响应数据
     */
    public void handleBin(WebSocketRequest request, WebSocketResponse response, byte[] data){
        APIResponse apiResponse;
        try {
            apiResponse = APIResponse.fromBin(data);
        } catch (Exception e) {
            log.error("Data conversion failed.",e);
            return;
        }
        handle(request,response,apiResponse);
    }

    /**
     * 处理WebSocket请求的核心方法
     * 根据API响应数据的关键字进行路由，并执行相应的回调
     * @param request WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data API响应数据对象
     */
    protected void handle(WebSocketRequest request, WebSocketResponse response, APIResponse data){
        if (StrUtil.isBlank(data.getKey())){
            executeCallback(request, response, data, root);
            return;
        }
        List<String> link = SplitUtil.split(data.getKey(),".",true,false);
        RPCRouterNode cacheNode = root;
        for (String s : link) {
            cacheNode = cacheNode.getChildren().get(s);
            if (cacheNode == null){
                break;
            }
        }
        if (cacheNode == null){
            response.sendTextMessage(APIResponse.error404("No matching APIs found").UUID(data.getUUID()).toString());
            return;
        }
        executeCallback(request, response, data, cacheNode);
    }

    /**
     * 执行回调函数
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data API响应数据
     * @param root RPC路由树的根节点
     */
    private void executeCallback(WebSocketRequest request, WebSocketResponse response, APIResponse data, RPCRouterNode root) {
        executorService.execute(() -> {
            Optional<APIResponse> result = root.getRpcServer().handle(request, response, data);
            result.ifPresent(apiResponse -> {
                if (!simpleRPC.isBinaryFirst()) {
                    response.sendTextMessage(apiResponse.toString());
                } else {
                    try {
                        response.sendBinaryMessage(apiResponse.toBin());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to convert data", e);
                    }
                }
            });
        });
    }

    /**
     * 添加路由节点
     *
     * @param key 路由键，用于标识节点路径
     * @param rpcServer 关联的RPC服务器实例
     */
    public void addRouterNode(String key, RPCServer rpcServer) {
        if (StrUtil.isBlank(key)) {
            log.error("It is not allowed to override the root node");
            return;
        }
        List<String> link = SplitUtil.split(key, ".", true, false);
        RPCRouterNode cacheNode = root;
        for (String s : link) {
            cacheNode = cacheNode.getChildren().computeIfAbsent(s, k -> RPCRouterNode.create(s));
        }
        cacheNode.setRpcServer(rpcServer);
    }

    /**
     * 添加回调
     *
     * @param uuid 回调的唯一标识符
     * @param rpcServer 回调的RPC服务器实例
     */
    public void addSandCallBack(String uuid, RPCServer rpcServer) {
        taskCache.put(uuid, rpcServer);
    }

    /**
     * 发送数据并回调
     *
     * @param response WebSocket响应对象
     * @param data API响应数据
     * @param isBinary 是否以二进制方式发送
     * @param rpcServer 相关的RPC服务器实例
     */
    public void sendAndCallBack(WebSocketResponse response, APIResponse data, boolean isBinary, RPCServer rpcServer) {
        addSandCallBack(data.getUUID(), rpcServer);
        if (isBinary) {
            try {
                response.sendBinaryMessage(data.toBin());
            } catch (JsonProcessingException e) {
                log.error("Data conversion failed.", e);
                taskCache.remove(data.getUUID());
            }
        } else {
            response.sendTextMessage(data.toString());
        }
    }
}
