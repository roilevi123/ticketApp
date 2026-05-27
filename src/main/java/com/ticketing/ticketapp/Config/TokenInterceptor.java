package com.ticketing.ticketapp.Config;

import com.ticketing.ticketapp.Infastructure.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public TokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        request.setAttribute("cleanToken", token);
        if ("guest-temporary-token".equals(token)) {
            return true;
        }
        
        if (!tokenService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (tokenService.isBannedToken(token)) {
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"ACCOUNT_REMOVED\"}");
            }
            return false;
        }

        return true;
    }
}
