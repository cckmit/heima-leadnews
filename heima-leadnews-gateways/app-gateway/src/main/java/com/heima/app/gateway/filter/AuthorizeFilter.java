package com.heima.app.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.heima.app.gateway.util.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/4 9:05
 **/
@Component
@Slf4j
@Order(0)
public class AuthorizeFilter implements GlobalFilter {
    private static List<String> allowUrls = new ArrayList<>();// 放行的白名单路径
    static {
        allowUrls.add("/login/in"); // 登录
        allowUrls.add("/v2/api-docs");
        allowUrls.add("/login_auth");
    }
    /**
     * 过滤方法
     * @param exchange 封装了 请求  和  响应对象
     * @param chain  链对象
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 通过exchange获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        // 2. 检查请求路径是否需要拦截
        String path = request.getURI().getPath(); // 请求路径
        for (String allowUrl : allowUrls) {
            if (path.contains(allowUrl)) { // 说明不需要拦截
                //      不需要拦截直接放行
                return chain.filter(exchange); // 放行
            }
        }
        // 3. 获取请求头中的token信息
        String token = request.getHeaders().getFirst("token");
        //      如果token不存在  直接终止请求  返回401
        if (StringUtils.isBlank(token)) {
            // 终止请求
            return writeMessage(exchange,"请登录后再访问");
        }
        // 4. 解析token
        try {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            int i = AppJwtUtil.verifyToken(claimsBody);//-1：有效，0：有效，1：过期，2：过期
            if(i < 1){ // 有效
                Object id = claimsBody.get("id"); // 获取登录用户id
                //    解析成功
                //      获取token中存储的用户id   将用户id写入到请求头中 路由给其它微服务
                request.mutate().header("userId",String.valueOf(id));
                exchange.mutate().request(request);
                log.info("解析token成功  当前登录用户id ==> {}",id);
                return chain.filter(exchange);
            }
            log.error("解析token出现异常  ==>token已过期");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("解析token出现异常  ==>{}",e.getMessage());
            //    解析失败
        }
        //      终止请求  返回401
        return writeMessage(exchange,"解析用户token失败,请重新登录");

    }

    /**
     * SpringMVC + servlet + tomcat
     * SpringWebFlux + reactor + netty
     * @param exchange
     * @param msg
     * @return
     */
    private Mono<Void> writeMessage(ServerWebExchange exchange, String msg) {
        Map map = new HashMap<>();
        map.put("code","401");
        map.put("errorMessage",msg);
        // 获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        // 设置响应状态码
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 设置响应的数据格式
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 设置响应数据
        DataBuffer wrap = response.bufferFactory().wrap(JSON.toJSONBytes(map));
        // 响应数据
        return response.writeWith(Flux.just(wrap));
    }
}
