package lv.pawsitter.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("Response: {} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
            MDC.clear();
        }
    }
}