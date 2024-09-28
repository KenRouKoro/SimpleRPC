package com.foxapplication.simplerpc.server;

import com.foxapplication.simplerpc.common.APIResponse;
import com.foxapplication.simplerpc.common.HttpResponseUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.smartboot.http.server.*;
import org.smartboot.http.server.handler.HttpRouteHandler;
import org.smartboot.http.server.handler.WebSocketRouteHandler;

import java.io.IOException;

/**
 * SimpleRPC类，提供了一个简单的RPC服务器实现，支持WebSocket和HTTP协议。
 */
@Slf4j
public class SimpleRPC {

    /**
     * WebSocket路由处理器。
     */
    @Getter
    private final WebSocketRouteHandler websocketHandle = new WebSocketRouteHandler();

    /**
     * WebSocket路由链接，默认为"/ws"。
     */
    @Getter
    @Setter
    private String routeLink = "/ws";

    /**
     * RPC路由器实例。
     */
    private final RPCRouter rpcRouter = new RPCRouter(this);

    /**
     * 服务器端口号，默认为8080。
     */
    @Getter
    @Setter
    private int port = 8080;

    /**
     * HTTP引导程序实例。
     */
    @Getter
    private HttpBootstrap bootstrap;

    /**
     * HTTP路由处理器。
     */
    @Getter
    private final HttpRouteHandler httpHandle = new HttpRouteHandler();

    /**
     * 认证令牌，默认为空字符串。
     */
    @Getter
    @Setter
    private String token = StrUtil.EMPTY;

    /**
     * 标识服务器是否已初始化。
     */
    @Getter
    private boolean isInit = false;

    /**
     * 标识是否优先发送二进制消息。
     */
    @Getter
    @Setter
    private boolean binaryFirst = false;

    /**
     * 默认构造函数。
     */
    public SimpleRPC() {
    }

    /**
     * 带参数的构造函数。
     *
     * @param routeLink WebSocket路由链接
     * @param port 服务器端口号
     */
    public SimpleRPC(String routeLink, int port) {
        this.routeLink = routeLink;
        this.port = port;
    }

    /**
     * 初始化RPC服务器，配置WebSocket和HTTP处理器。
     */
    public void init() {
        log.info("SimpleRPC init");
        websocketHandle.route(routeLink, new WebSocketHandlerImpl(this) {
            @Override
            public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {
                rpcRouter.handle(request, response, data);
            }

            @Override
            public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {
                rpcRouter.handleBin(request, response, data);
            }
        });
        httpHandle.route("/", new HttpServerHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response) throws IOException {
                HttpResponseUtil.addJsonResponseHeader(response);
                response.write(APIResponse.success("SimpleRPC").toHex());
            }
        });

        bootstrap = new HttpBootstrap();
        bootstrap.webSocketHandler(websocketHandle);
        bootstrap.httpHandler(httpHandle);
        bootstrap.configuration().bannerEnabled(false);
        isInit = true;
    }

    /**
     * 启动RPC服务器。
     */
    public void start() {
        if (!isInit) {
            log.error("SimpleRPC not init");
            return;
        }
        log.info("SimpleRPC start");

        rpcRouter.start();

        bootstrap.setPort(port);
        bootstrap.start();
    }

    /**
     * 停止RPC服务器。
     */
    public void stop() {
        log.info("SimpleRPC stop");
        bootstrap.shutdown();
    }
}
