package com.heima.gateway.filter;

import com.heima.gateway.util.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


/**
 * 认证过滤器
 *
 * @author limingfei
 */
@Component
@Slf4j
public class AuthorizeFilter implements GlobalFilter {
    private static List<String> urlList = new ArrayList<>();

    // 初始化白名单 url路径
    static {
        urlList.add("/login/in");
        urlList.add("/v2/api-docs");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取请求对象 响应对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //**** 2. 判断当前请求路径是否放行
        String reqUrl = request.getURI().getPath();
        for (String url : urlList) {
            if (reqUrl.contains(url)) {
                return chain.filter(exchange);
            }
        }
        // 3. 获取请求头中的token值
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.isBlank(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        // 4. 检查token是否有效
        try {
            Claims claims = AppJwtUtil.getClaimsBody(token);
            // 校验有效
            if (AppJwtUtil.verifyToken(claims) < 1) {
                // 5. 获取token中存储的用户ID
                Integer id = claims.get("id", Integer.class);
                // 6. 获取到的id值，重写到请求头中，传递到要调用的微服务中
                request.mutate().header("userId", String.valueOf(id)).build();
                exchange.mutate().request(request).build();
                return chain.filter(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
