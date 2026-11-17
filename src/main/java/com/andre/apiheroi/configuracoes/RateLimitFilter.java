package com.andre.apiheroi.configuracoes;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long QUANTIDADE = 20;
    private static final Duration DURACAO = Duration.ofMinutes(1);
    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();

    private Bucket novoBalde() {
        return Bucket.builder()
                .addLimit(limite -> limite
                        .capacity(QUANTIDADE)
                        .refillIntervally(QUANTIDADE, DURACAO))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket balde = baldes.computeIfAbsent(ipDoCliente(request), ip -> novoBalde());
        ConsumptionProbe consumo = balde.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-Rate-Limit-Limit", String.valueOf(QUANTIDADE));

        if (consumo.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(consumo.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long esperar = TimeUnit.NANOSECONDS.toSeconds(consumo.getNanosToWaitForRefill()) + 1;
            response.setHeader("Retry-After", String.valueOf(esperar));
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"erro\":\"Muitas requisições. Tente novamente em " + esperar + "s\"}");
        }
    }

    private String ipDoCliente(HttpServletRequest request) {
        // Sem um proxy confiavel configurado, o cliente pode falsificar X-Forwarded-For.
        return request.getRemoteAddr();
    }
}
