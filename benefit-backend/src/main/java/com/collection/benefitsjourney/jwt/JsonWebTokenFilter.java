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
    protected boolean shouldNotFilter(
            HttpServletRequest request)
            throws ServletException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("JWT Filter URI = " + uri);

        return uri.equals("/auth/register")
                || uri.equals("/auth/login")
                || uri.equals("/auth/google")
                || uri.equals("/auth/password/forgot")
                || uri.equals("/auth/password/reset")
                || uri.matches("/users/\\d+")
                || uri.matches("/users/\\d+/posts")

                // 影城 API 白名單
                || (uri.equals("/cinemas") && (method.equals("GET")))
                || (uri.matches("/cinemas/\\d+") && method.equals("GET"))
                || (uri.equals("/cinemas/map") && method.equals("GET"))

                // 電影 API 白名單
                || (uri.equals("/movies") && method.equals("GET"))
                || (uri.matches("/movies/\\d+") && method.equals("GET"))

                // 交換刊登 API 白名單：只允許公開 GET 查詢
                || (uri.equals("/exchange-listings")
                        && method.equals("GET"))
                || (uri.matches("/exchange-listings/\\d+")
                        && method.equals("GET"))

                // 票券刊登 API 白名單：只允許公開 GET 查詢
                || (uri.equals("/ticket-listings")
                        && method.equals("GET"))
                || (uri.matches("/ticket-listings/\\d+")
                        && method.equals("GET"))

                // Swagger 白名單
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/swagger-ui/")
                || uri.equals("/v3/api-docs")
                || uri.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        System.out.println(
                "Authorization = " + auth);

        // 沒有 Authorization
        if (auth == null || !auth.startsWith("Bearer ")) {

            response.setStatus(
                    HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        // 取得 JWT
        String token = auth.substring(7);

        // 驗證 JWT
        String subject = jsonWebTokenUtility.validateToken(token);

        // JWT 驗證成功
        if (subject != null) {

            request.setAttribute("usersId", Integer.valueOf(subject));

            filterChain.doFilter(request, response);

            return;
        }

        // JWT 驗證失敗
        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN);
    }
}