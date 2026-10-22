package com.collection.benefitsjourney.jwt;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.UsersRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonWebTokenFilter extends OncePerRequestFilter {

    private final JsonWebTokenUtility jsonWebTokenUtility;
    private final UsersRepository usersRepository;

    public JsonWebTokenFilter(JsonWebTokenUtility jsonWebTokenUtility, UsersRepository usersRepository) {
        this.jsonWebTokenUtility = jsonWebTokenUtility;
        this.usersRepository = usersRepository;
    }

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
                || (uri.matches("/users/\\d+") && method.equals("GET"))
                || (uri.matches("/users/\\d+/posts") && method.equals("GET"))

                // 影城 API 白名單
                || (uri.equals("/cinemas/map") && method.equals("GET"))
                || (uri.equals("/cinemas/explore") && method.equals("GET"))

                // 電影 API 白名單
                || (uri.equals("/movies/explore") && method.equals("GET"))

                // 特典 API 白名單
                || (uri.equals("/benefits") && method.equals("GET"))
                || (uri.matches("/benefits/\\d+") && method.equals("GET"))
                || (uri.matches("/movies/\\d+/benefits") && method.equals("GET"))
                || (uri.equals("/benefits/calendar") && method.equals("GET"))
                || (uri.equals("/benefits/calendar") && method.equals("GET"))
                || (uri.equals("/users/report-ranking") && method.equals("GET"))

                // 庫存狀態 API 白名單（地圖頁公開查詢）
                || (uri.matches("/benefits/\\d+/cinemas/status") && method.equals("GET"))
                || (uri.matches("/cinemas/\\d+/benefits/status") && method.equals("GET"))
                || (uri.matches("/movies/\\d+/benefits/status") && method.equals("GET"))

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

    // 驗證 JWT
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println(">>> doFilterInternal ENTER <<<");

        response.setHeader("X-Debug-Filter", "JsonWebTokenFilter");

        String auth = request.getHeader("Authorization");

        System.out.println("=== 進入 doFilterInternal ===");
        System.out.println("Method = " + request.getMethod());
        System.out.println("Authorization exists = " + (auth != null));

        // 沒有 Bearer Token
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED, "請先登入");
            return;
        }

        String token = auth.substring(7);
        System.out.println("=== 開始驗證 JWT ===");
        String subject = jsonWebTokenUtility.validateToken(token);
        System.out.println("JWT subject = " + subject);

        if (subject == null || subject.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token 無效或已過期");
            return;
        }
        final Integer usersId;
        // 確認 subject 可以轉成會員 ID
        try {
            usersId = Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Token 會員 ID 格式錯誤");
            return;
        }
        // 每次請求都確認會員目前的資料庫狀態
        UsersBean usersBean = usersRepository.findById(usersId)
                .orElse(null);

        if (usersBean == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "會員不存在");
            return;
        }

        String status = usersBean.getStatus();

        System.out.println("JWT User Status = [" + status + "]");
        System.out.println("JWT Filter Method = [" + request.getMethod() + "]");

        // 已停權或已刪除的會員不能使用舊 Token
        if ("BANNED".equals(status) || "DELETED".equals(status)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "此帳號目前無法使用");
            return;
        }

       
        // 已停用會員可以查詢自己的資料，以及呼叫自己的狀態 API 重新啟用帳號
        boolean isMyProfileApi = request.getMethod().equals("GET")
                && request.getRequestURI().equals("/users/me");

        boolean isMyStatusApi = request.getMethod().equals("PATCH")
                && request.getRequestURI().equals("/users/me/status");

        boolean isLogoutApi = request.getMethod().equals("POST")
                && request.getRequestURI().equals("/auth/logout");

        System.out.println("isMyProfileApi = " + isMyProfileApi);
        System.out.println("isMyStatusApi = " + isMyStatusApi);
        System.out.println("isLogoutApi = " + isLogoutApi);

        if ("INACTIVE".equals(status)
                && !isMyProfileApi
                && !isMyStatusApi
                && !isLogoutApi) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "此帳號已停用，請先重新啟用");
            return;
        }

        // 將會員 ID 傳給 Controller
        request.setAttribute("usersId", usersId);

        // 設定 Spring Security 的登入身分
        String role = usersBean.getRole();

        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "會員角色無效");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + role)));

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);

    }
}