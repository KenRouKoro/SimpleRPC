package com.foxapplication.simplerpc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.smartboot.http.common.codec.websocket.CloseReason;
import org.smartboot.http.common.utils.WebSocketUtil;
import org.smartboot.http.server.WebSocketHandler;
import org.smartboot.http.server.WebSocketRequest;
import org.smartboot.http.server.WebSocketResponse;
import org.smartboot.http.server.impl.WebSocketRequestImpl;
import org.smartboot.http.server.impl.WebSocketResponseImpl;
import org.smartboot.socket.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
@Slf4j
public class WebSocketHandlerImpl extends WebSocketHandler {
    private final SimpleRPC simpleRPC;
    public WebSocketHandlerImpl(SimpleRPC simpleRPC) {
        this.simpleRPC = simpleRPC;
    }
    @Override
    public void whenHeaderComplete(WebSocketRequestImpl request, WebSocketResponseImpl response) {
        onHandShake(request, response);
    }

    @Override
    public final void handle(WebSocketRequest request, WebSocketResponse response) throws IOException {
        try {
            switch (request.getFrameOpcode()) {
                case WebSocketUtil.OPCODE_TEXT:
                    handleTextMessage(request, response, new String(request.getPayload(), StandardCharsets.UTF_8));
                    break;
                case WebSocketUtil.OPCODE_BINARY:
                    handleBinaryMessage(request, response, request.getPayload());
                    break;
                case WebSocketUtil.OPCODE_CLOSE:
                    try {
                        onClose(request, response, new CloseReason(request.getPayload()));
                    } finally {
                        response.close();
                    }
                    break;
                case WebSocketUtil.OPCODE_PING:
                    handlePing(request, response);
                    break;
                case WebSocketUtil.OPCODE_PONG:
                    handlePong(request, response);
                    break;
                case WebSocketUtil.OPCODE_CONTINUE:
                    log.warn("unSupport OPCODE_CONTINUE now,ignore payload: {}", StringUtils.toHexString(request.getPayload()));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        } catch (Throwable throwable) {
            onError(request, throwable);
            throw throwable;
        }
    }

    /**
     * 处理WebSocket的ping请求
     * 当服务器接收到客户端的ping请求时，此方法将被调用
     *
     * @param request  WebSocket请求，包含客户端发送的ping数据
     * @param response 用于向客户端发送响应的WebSocket响应对象
     */
    public void handlePing(WebSocketRequest request, WebSocketResponse response) {
        // 响应客户端的ping请求，使用接收到的payload数据进行pong回应
        response.pong(request.getPayload());
    }

    /**
     * 处理WebSocket心跳响应（Pong）
     * 当WebSocket连接收到服务器发送的心跳响应（Pong）时，此方法被调用
     * 主要用于处理心跳机制，保持连接的活性，防止连接超时或无数据交换导致的断开
     *
     * @param request 传入的WebSocket请求对象
     * @param response 传入的WebSocket响应对象
     */
    public void handlePong(WebSocketRequest request, WebSocketResponse response) {
        log.debug("receive pong...");
    }


    /**
     * 处理WebSocket握手
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     */
    public void onHandShake(WebSocketRequest request, WebSocketResponse response) {
        if (!StrUtil.isBlank(simpleRPC.getToken())){
            String token = StrUtil.EMPTY;
            try {
                token = request.getParameters().get("token")[0];
            }catch (Exception e){
                log.error("Request token is null , from:[{}]",request.getLocalAddress().toString());
            }
            if (!token.equals(simpleRPC.getToken())){
                log.error("Request token is not match , handShake stop , id:[{}],from:[{}] ",token,request.getLocalAddress().toString());
                response.close(403,"Auth fail");
            }
        }
        log.debug("handShake success");
    }


    /**
     * 连接关闭
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     */
    public void onClose(WebSocketRequest request, WebSocketResponse response, CloseReason closeReason) {
        log.debug("close connection");
    }

    /**
     * 处理字符串请求消息
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data WebSocket负载字符串
     */
    public void handleTextMessage(WebSocketRequest request, WebSocketResponse response, String data) {

    }

    /**
     * 处理二进制请求消息
     *
     * @param request  WebSocket请求对象
     * @param response WebSocket响应对象
     * @param data WebSocket负载二进制数据
     */
    public void handleBinaryMessage(WebSocketRequest request, WebSocketResponse response, byte[] data) {

    }

    /**
     * 处理WebSocket连接过程中的异常
     *
     * @param request WebSocket请求对象
     * @param throwable 发生的异常对象
     */
    public void onError(WebSocketRequest request, Throwable throwable) {
        log.error("WebSocket error", throwable);
    }

}
