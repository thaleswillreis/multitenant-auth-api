package com.thaleswillreis.authapi.ratelimit;

import com.thaleswillreis.authapi.config.RateLimitProperties;
import com.thaleswillreis.authapi.util.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String EXCLUDED_PATH = "/health";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitFilter(ProxyManager<String> proxyManager, RateLimitProperties rateLimitProperties) {
        this.proxyManager = proxyManager;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (EXCLUDED_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed = true;
        long remaining = rateLimitProperties.getCapacity();
        long waitSeconds = 0;

        try {
            String clientIp = ClientIpResolver.resolve(request);
            Bucket bucket = proxyManager.getProxy("rate-limit:" + clientIp, this::bucketConfiguration);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            allowed = probe.isConsumed();
            remaining = probe.getRemainingTokens();
            waitSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
        } catch (RuntimeException ex) {
            // Fail-open: se o Redis estiver indisponivel, o rate limiter nao deve derrubar
            // a API.
            allowed = true;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitProperties.getCapacity()));

        if (allowed) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Limite de requisicoes excedido. Tente novamente em breve.\"}");
    }

    private BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rateLimitProperties.getCapacity())
                        .refillGreedy(rateLimitProperties.getCapacity(),
                                Duration.ofSeconds(rateLimitProperties.getRefillDurationSeconds()))
                        .build())
                .build();
    }

}