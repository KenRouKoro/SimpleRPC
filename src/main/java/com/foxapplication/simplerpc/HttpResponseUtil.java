package com.foxapplication.simplerpc;

import org.smartboot.http.server.HttpResponse;

public class HttpResponseUtil {

    /**
     * 添加JSON响应头
     *
     * @param response Http响应对象，用于添加内容类型头
     */
    public static void addJsonResponseHeader(HttpResponse response){
        // 添加内容类型为application/json的响应头
        response.addHeader("Content-Type", "application/json");
    }

}
