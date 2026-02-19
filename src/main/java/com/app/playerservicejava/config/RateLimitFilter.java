package com.app.playerservicejava.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // ✅ One bucket per IP address
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // ✅ 10 writes per minute per IP
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucketForIp(String ip) {
        return buckets.computeIfAbsent(ip, k -> createNewBucket());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // ✅ Only rate limit write operations on /v1/players
        boolean isWriteOperation = (method.equals("POST") || method.equals("PUT") ||
                method.equals("PATCH") || method.equals("DELETE"))
                && path.startsWith("/v1/players");

        if (isWriteOperation) {
            String ip = getClientIp(request);
            Bucket bucket = getBucketForIp(ip);

            if (bucket.tryConsume(1)) {
                // ✅ Token consumed - request allowed
                response.addHeader("X-RateLimit-Remaining",
                        String.valueOf(bucket.getAvailableTokens()));
                filterChain.doFilter(request, response);
            } else {
                // ❌ No tokens left - reject request
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"Too many requests. Limit: 10 writes/min per IP. Try again later.\"}"
                );
            }
        } else {
            // ✅ GET requests - no rate limiting
            filterChain.doFilter(request, response);
        }
    }

    // ✅ Handle proxies/load balancers
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
