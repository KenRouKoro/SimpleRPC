package com.foxapplication.simplerpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.data.id.IdUtil;
import org.dromara.hutool.core.text.StrUtil;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class APIResponse {
    /**
     * 定义一个序列化器字段，用于将APIResponse对象转换为JSON字符串
     */
    @Getter
    private final static ObjectMapper serializer = new ObjectMapper();
    /**
     * 定义一个Bson序列化器字段，用于将APIResponse对象转换为BSON格式的字节数组
     */
    @Getter
    private final static ObjectMapper bsonSerializer = new ObjectMapper(new BsonFactory());
    /**
     * 定义一个UUID字段，用于标识当前APIResponse实例的唯一标识符
     * 该字段初始化为空字符串
     */
    private String UUID = StrUtil.EMPTY;

    /**
     * 定义一个状态码字段，默认值为200，用于表示操作的成功或失败等状态
     */
    private Integer status = 200;

    /**
     * 定义一个消息字段，用于存储操作过程中的信息或错误消息
     * 该字段初始化为空字符串
     */
    private String message = StrUtil.EMPTY;

    /**
     * 定义一个路径字段
     * 该字段初始化为空字符串
     */
    private String key = StrUtil.EMPTY;

    /**
     * 定义一个请求对象字段，用于存储当前请求的相关信息
     * 该字段初始化为空字符串
     */
    private Object request = StrUtil.EMPTY;

    /**
     * 定义一个参数映射字段，用于存储调用过程中的各种参数
     */
    private Map<String,Object> params = new ConcurrentHashMap<>();
    //------------------------------类参数定义完-----------------------------------
    /**
     * 将APIResponse对象转换为JSON字符串
     *
     * @return 表示APIResponse的JSON字符串
     */
    public String toString(){
        try {
            return serializer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("APIResponse toString error",e);
            return StrUtil.EMPTY_JSON;
        }
    }
    /**
     * 将当前对象转换为十六进制字符串表示形式
     *
     * @return 字节数组，表示对象的十六进制字符串形式
     */
    public byte[] toStringHex(){
        return toString().getBytes();
    }
    /**
     * 将当前对象转换为BSON二进制字节数组
     *
     * @return 字节数组表示的当前对象的BSON编码
     * @throws JsonProcessingException 如果对象无法被序列化为BSON格式，则抛出此异常
     */
    public byte[] toHex() throws JsonProcessingException {
        return bsonSerializer.writeValueAsBytes(this);
    }
    /**
     * 生成一个唯一的UUID
     *
     * @return APIResponse对象，包含生成的UUID
     */
    public APIResponse createUUID(){
        // 生成一个快速UUID，并赋值给类成员UUID
        this.UUID = IdUtil.fastUUID();
        // 返回当前APIResponse对象，支持链式调用
        return this;
    }

    /**
     * 设置UUID
     *
     * @param uuid 要设置的UUID字符串
     * @return APIResponse对象，便于链式调用
     */
    public APIResponse UUID(String uuid){
        // 将传入的UUID字符串赋值给类成员UUID
        this.UUID = uuid;
        // 返回当前APIResponse对象，支持链式调用
        return this;
    }

    //------------------------------类工具方法完-----------------------------------
    /**
     * 创建一个APIResponse实例
     *
     * @return 新创建的APIResponse实例
     */
    public static APIResponse create(){
        return new APIResponse();
    }

    /**
     * 创建一个表示成功的APIResponse实例，并设置消息
     *
     * @param message 成功消息
     * @return 表示成功的APIResponse实例
     */
    public static APIResponse success(String message){
        APIResponse apiResponse = create();
        apiResponse.setMessage(message);
        return apiResponse;
    }

    /**
     * 创建一个表示成功的APIResponse实例，并设置消息和请求对象
     *
     * @param message 成功消息
     * @param request 请求对象
     * @return 表示成功的APIResponse实例
     */
    public static APIResponse success(String message,Object request){
        APIResponse apiResponse = success(message);
        apiResponse.setRequest(request);
        return apiResponse;
    }

    /**
     * 创建一个表示成功的APIResponse实例，并设置消息、请求对象和额外参数
     *
     * @param message 成功消息
     * @param request 请求对象
     * @param params 额外参数
     * @return 表示成功的APIResponse实例
     */
    public static APIResponse success(String message,Object request,Map<String,Object> params){
        APIResponse apiResponse = success(message, request);
        apiResponse.setParams(params);
        return apiResponse;
    }

    /**
     * 创建一个表示错误的APIResponse实例，并设置消息和状态码为500
     *
     * @param message 错误消息
     * @return 表示错误的APIResponse实例
     */
    public static APIResponse error(String message){
        APIResponse apiResponse = create();
        apiResponse.setStatus(500);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    /**
     * 创建一个表示错误的APIResponse实例，并设置消息、请求对象和状态码为500
     *
     * @param message 错误消息
     * @param request 请求对象
     * @return 表示错误的APIResponse实例
     */
    public static APIResponse error(String message,Object request){
        APIResponse apiResponse = error(message);
        apiResponse.setRequest(request);
        return apiResponse;
    }

    /**
     * 创建一个表示错误的APIResponse实例，并设置消息、请求对象、额外参数和状态码为500
     *
     * @param message 错误消息
     * @param request 请求对象
     * @param params 额外参数
     * @return 表示错误的APIResponse实例
     */
    public static APIResponse error(String message,Object request,Map<String,Object> params){
        APIResponse apiResponse = error(message, request);
        apiResponse.setParams(params);
        return apiResponse;
    }

    /**
     * 创建一个表示找不到资源的错误APIResponse实例，并设置消息和状态码为404
     *
     * @param message 错误消息
     * @return 表示找不到资源的错误APIResponse实例
     */
    public static APIResponse error404(String message){
        APIResponse apiResponse = create();
        apiResponse.setStatus(404);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    /**
     * 创建一个表示找不到资源的错误APIResponse实例，并设置消息、请求对象和状态码为404
     *
     * @param message 错误消息
     * @param request 请求对象
     * @return 表示找不到资源的错误APIResponse实例
     */
    public static APIResponse error404(String message,Object request){
        APIResponse apiResponse = error404(message);
        apiResponse.setRequest(request);
        return apiResponse;
    }

    /**
     * 创建一个表示找不到资源的错误APIResponse实例，并设置消息、请求对象、额外参数和状态码为404
     *
     * @param message 错误消息
     * @param request 请求对象
     * @param params 额外参数
     * @return 表示找不到资源的错误APIResponse实例
     */
    public static APIResponse error404(String message,Object request,Map<String,Object> params){
        APIResponse apiResponse = error404(message, request);
        apiResponse.setParams(params);
        return apiResponse;
    }
    /**
     * 将字符串转换为API响应对象
     *
     * @param jsonStr 一个包含API响应信息的JSON字符串
     * @return 返回一个根据提供的JSON字符串反序列化而得到的APIResponse对象
     */
    public static APIResponse fromStr(String jsonStr) throws JsonProcessingException {
        return serializer.readValue(jsonStr, APIResponse.class);
    }

    /**
     * 将十六进制编码的BSON字节数组转换为APIResponse对象
     *
     * @param bson 十六进制编码的BSON字节数组
     * @return 转换后的APIResponse对象
     * @throws IOException 如果无法读取或反序列化指定的字节数组，则抛出此异常
     */
    public static APIResponse fromHex(byte[] bson) throws IOException {
        return bsonSerializer.readValue(bson, APIResponse.class);
    }
}
