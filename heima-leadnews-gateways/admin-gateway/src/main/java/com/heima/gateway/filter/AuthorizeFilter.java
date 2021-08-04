package com.heima.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.heima.gateway.util.AppJwtUtil;
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
import java.util.Map;

/**
 *  认证过滤器
 * @author limingfei
 */
@Component
@Slf4j
@Order(0) // 值越小越优先执行
public class AuthorizeFilter implements GlobalFilter {
    //初始化白名单路径
    private static ArrayList<String> urlList = new ArrayList<>();
    static {
        urlList.add("/login/in");
        urlList.add("/v2/api-docs");
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1 判断当前是否是登录请求，如果是登录则放行
        ServerHttpRequest request = exchange.getRequest();
        String reqUrl = request.getURI().getPath();
        if (urlList.contains(reqUrl)) {
            return chain.filter(exchange);
        }
        //2 获取请求头jwt token信息
        String jwtToken = request.getHeaders().getFirst("token");
        //如果不存在，向客户端返回错误提示信息
        if (!StringUtils.isNotBlank(jwtToken)) {
           return writeMessage(exchange, "需要登录");
        }
        //3 判断令牌信息是否正确
        //  -1：有效，0：有效，1：过期，2：过期
        try {
            Claims claims = AppJwtUtil.getClaimsBody(jwtToken);
            int verifyToken = AppJwtUtil.verifyToken(claims);
            if (verifyToken>0) {
                return writeMessage(exchange, "认证时间已过，请重新登录");
            }
            //3.2 解析JWT令牌信息
            Integer id = claims.get("id", Integer.class);
            log.info("token网关校验成功------id：{},------URL:{}",id,request.getURI().getPath());
            //***4 将令牌信息传递到对应的微服务
            request.mutate().headers(httpHeaders -> {
                httpHeaders.add("userId",String.valueOf(id));
            });
            //5 返回结果
            return chain.filter(exchange);
        } catch (Exception e) {
           log.error("token校验失败：{}",e);
           return writeMessage(exchange,"认证时间已过，请重新登录");
        }
    }
    /**
     * 返回错误提示信息
     * @return
     */
    private Mono<Void> writeMessage(ServerWebExchange exchange, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("errorMessage", message);
        //获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        //设置状态码
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        //response.setStatusCode(HttpStatus.OK);
        //设置返回类型
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //设置返回数据
        DataBuffer buffer = response.bufferFactory().wrap(JSON.toJSONBytes(map));
        //响应数据回浏览器
        return response.writeWith(Flux.just(buffer));
    }

}
