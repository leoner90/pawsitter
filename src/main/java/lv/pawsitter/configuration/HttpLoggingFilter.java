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

/**
 * Servlet filter that logs every incoming HTTP request and its corresponding response.
 *
 * <p>This filter generates a unique {@code requestId} for each request and stores it in the
 * Mapped Diagnostic Context (MDC), allowing all log entries produced during the request
 * lifecycle to be correlated. The ID is cleared after the response is logged.</p>
 *
 * <p>Logged information includes:</p>
 * <ul>
 *     <li>HTTP method and request URI at the moment the request enters the filter chain</li>
 *     <li>HTTP method, request URI, and response status code after the request is processed</li>
 * </ul>
 *
 * <p>The filter extends {@link org.springframework.web.filter.OncePerRequestFilter}, ensuring
 * that it is executed exactly once per request.</p>
 *
 * <p><strong>Threading note:</strong> MDC is thread‑local. If asynchronous processing or
 * delegation to other threads occurs, the requestId may need to be propagated manually.</p>
 *
 * @author Александр
 * @see org.springframework.web.filter.OncePerRequestFilter
 */
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
            log.info("Response: {} {} -> {}", request.getMethod(),
                    request.getRequestURI(), response.getStatus());
            MDC.clear();
        }
    }
}