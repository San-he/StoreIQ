package com.storeiq.config;

import com.storeiq.common.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    /** 无需认证的路径 */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/stores/map"   // 地图首页公开访问
    );

    /** 静态资源路径前缀 */
    private static final List<String> STATIC_PREFIXES = Arrays.asList(
            "/swagger", "/v2/api-docs", "/doc.html", "/webjars"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 跳过无需认证的路径
        if (isExcluded(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头获取 Token
        String token = extractToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\"}");
            return;
        }

        // 将用户信息存入 request attribute，供 Controller 使用
        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("username", jwtUtil.getUsername(token));
        request.setAttribute("role", jwtUtil.getRole(token));

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 判断路径是否跳过认证
     */
    private boolean isExcluded(String path) {
        for (String exclude : EXCLUDE_PATHS) {
            if (path.startsWith(exclude)) return true;
        }
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }
}
