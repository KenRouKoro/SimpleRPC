package com.foxapplication.simplerpc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.foxapplication.simplerpc.common.APIResponse;
import com.foxapplication.simplerpc.common.RPCRouterNode;
import com.foxapplication.simplerpc.common.TimedCache;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.net.url.UrlBuilder;
import org.dromara.hutool.core.net.url.UrlUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.text.split.SplitUtil;
import org.dromara.hutool.core.thread.ExecutorBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.smartboot.http.server.WebSocketRequest;
import org.smartboot.http.server.WebSocketResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
public class SimpleRPCClient {
    @Setter
    @Getter
    private String url;
    /**
     *  超时时间
     */
    @Setter
    @Getter
    private long timeout = 60 * 2 * 1000;
    @Getter
    private WebSocketClient webSocketClient = null;
    private final TimedCache<String, RPCServer> cache = new TimedCache<>();
    @Getter
    private final RPCRouterNode<RPCServer> root = new RPCRouterNode<>();
    @Getter
    @Setter
    private boolean ssl = false;
    @Getter
    @Setter
    private String token = StrUtil.EMPTY;
    @Getter
    private boolean standby = false;
    @Getter
    @Setter
    private boolean binaryFirst = false;
    private final ExecutorService executorService = ExecutorBuilder.of()
            .setCorePoolSize(1)
            .setMaxPoolSize(1)
            .setKeepAliveTime(0)
            .build();

    public SimpleRPCClient(String url) {
        this.url = url;
        // 设置根路由的RPC服务器处理逻辑
        root.setRpcServer((data)->{
            // 从任务缓存中获取与请求UUID关联的RPC服务器实例
            RPCServer server = cache.get(data.getUUID());
            if (server != null){
                // 如果找到匹配的RPC服务器实例，则从缓存中移除，以避免重复处理
                cache.remove(data.getUUID());
                // 调用找到的RPC服务器实例处理请求并返回处理结果
                return server.handle(data);
            }
            // 如果没有找到匹配的RPC服务器实例，返回空的Optional
            return Optional.empty();
        });
        cache.setCallback((key, value)->{
            APIResponse response = APIResponse.create().UUID(key);
            response.setStatus(408);
            response.setMessage("Request timeout");
            value.handle(response);
        });
    }
    public void init(){
        String ws_url = UrlBuilder.of()
                .setScheme(ssl ? "wss" : "ws")
                .setHost(url)
                .addQuery("token",token)
                .build();
        webSocketClient = new WebSocketClient(UrlUtil.toURI(ws_url)) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                standby=true;
            }

            @Override
            public void onMessage(String s) {
                handleStr(s);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                handleBin(bytes.array());
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                standby=false;
            }

            @Override
            public void onError(Exception e) {
                log.error("WebSocket connection failed.",e);
                standby=false;
            }
        };
        root.setRpcServer(data->{
            RPCServer server = cache.get(data.getKey());
            if (server != null){
                return server.handle(data);
            }
            return Optional.empty();
        });
    }
    public void start() throws IOException {
        webSocketClient.connect();
        cache.startCleanupTask();
    }

    public void handleBin(byte[] data){
        APIResponse apiResponse;
        try {
            apiResponse = APIResponse.fromBin(data);
        } catch (Exception e) {
            log.error("Data conversion failed.",e);
            return;
        }
        handle(apiResponse);
    }
    public void handleStr(String data){
        APIResponse apiResponse;
        try {
            apiResponse = APIResponse.fromStr(data);
        } catch (JsonProcessingException e) {
            log.error("Data conversion failed.",e);
            return;
        }
        handle(apiResponse);
    }

    private void handle(APIResponse data){
        if (StrUtil.isBlankIfStr(data.getKey())){
            executorService.execute(()->{
                Optional<APIResponse> result = root.getRpcServer().handle(data);
                result.ifPresent(this::send);
            });
            return;
        }
        executorService.execute(()->{
            List<String> keys = SplitUtil.split(data.getKey(), ".");
            RPCRouterNode<RPCServer> cacheNode = root;
            for (String s : keys) {
                cacheNode = cacheNode.getChildren().get(s);
                if (cacheNode == null){
                    break;
                }
            }
            if (cacheNode == null){
                send(APIResponse.error404("No matching APIs found").UUID(data.getUUID()));
                return;
            }
            Optional<APIResponse> result = cacheNode.getRpcServer().handle(data);
            result.ifPresent(this::send);
        });
    }

    public void stop(){
        if (webSocketClient == null){
            return;
        }
        webSocketClient.close();
        cache.shutdown();
    }
    public void send(APIResponse data){
        send(data,binaryFirst);
    }
    public void send(APIResponse data,boolean bin){
        if (bin) {
            try {
                webSocketClient.send(data.toBin());
            } catch (JsonProcessingException e) {
                log.error("Data conversion failed.", e);
                cache.remove(data.getUUID());
            }
        } else {
            webSocketClient.send(data.toString());
        }
    }
    /**
     * 添加回调
     *
     * @param uuid 回调的唯一标识符
     * @param rpcServer 回调的RPC服务器实例
     */
    public void addSendCallBack(String uuid,RPCServer rpcServer) {
        cache.put(uuid, rpcServer);
    }
    /**
     * 发送数据并回调
     *
     * @param data API响应数据
     * @param isBinary 是否以二进制方式发送
     * @param rpcServer 相关的RPC服务器实例
     */
    public void sendAndCallBack( APIResponse data, boolean isBinary, RPCServer rpcServer) {
        addSendCallBack(data.getUUID(), rpcServer);
        send(data, isBinary);
    }
    public void sendAndCallBack(APIResponse data,RPCServer rpcServer) {
        addSendCallBack(data.getUUID(), rpcServer);
        send(data);
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
        RPCRouterNode<RPCServer> cacheNode = root;
        for (String s : link) {
            cacheNode = cacheNode.getChildren().computeIfAbsent(s, k -> RPCRouterNode.create(s));
        }
        cacheNode.setRpcServer(rpcServer);
    }
    public void removeRouterNode(String key) {
        if (StrUtil.isBlank(key)) {
            log.error("Cannot remove root");
            return;
        }
        List<String> link = SplitUtil.split(key, ".", true, false);
        RPCRouterNode<RPCServer> cacheNode = root;
        RPCRouterNode<RPCServer> lastNode = null;
        String findKey = null;
        for (String s : link) {
            lastNode = cacheNode;
            findKey = s;
            cacheNode = cacheNode.getChildren().get(s);
            if (cacheNode == null){
                return;
            }
        }
        if (lastNode == null || findKey == null){
            return;
        }
        lastNode.getChildren().remove(findKey);

    }
}
