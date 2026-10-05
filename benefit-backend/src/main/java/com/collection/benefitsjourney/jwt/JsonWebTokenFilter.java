package com.collection.benefitsjourney.jwt;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class JsonWebTokenFilter extends OncePerRequestFilter {
    private final JsonWebTokenUtility jsonWebTokenUtility;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        if("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        if(auth!=null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            String subject = jsonWebTokenUtility.validateToken(token);
            if(subject!=null) {
                // 權限控管程式
                filterChain.doFilter(request, response);
                return ;
            }
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        return;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        // uri.startsWith("/ajax/pages/products") || uri.startsWith("/rest/pages/products")
        return uri.startsWith("/ajax/secure") ||
                uri.startsWith("/pages/detail");
    }
}
