package com.mentaiko.backend.security;

import java.io.IOException;
import java.time.Duration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class PasswordResetRateLimitFilter extends OncePerRequestFilter {
    private final PasswordResetRateLimiter limiter;
    public PasswordResetRateLimitFilter(PasswordResetRateLimiter limiter) { this.limiter = limiter; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean forgot = path.equals("/api/auth/forgot-password");
        if (!request.getMethod().equals("POST") || (!forgot && !path.equals("/api/auth/reset-password"))) {
            chain.doFilter(request, response);
            return;
        }
        // Never trust arbitrary X-Forwarded-For. Nginx should also limit by its client IP.
        Duration window = Duration.ofMinutes(15);
        if (!limiter.acquire(path + ":ip:" + request.getRemoteAddr(), forgot ? 10 : 30, window)
                || !limiter.acquire(path + ":global", forgot ? 100 : 300, window)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "900");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intenta de nuevo mas tarde.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
