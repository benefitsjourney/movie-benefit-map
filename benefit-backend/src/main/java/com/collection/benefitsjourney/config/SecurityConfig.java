package com.collection.benefitsjourney.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.collection.benefitsjourney.jwt.JsonWebTokenFilter;

@Configuration
public class SecurityConfig {
    private final JsonWebTokenFilter jsonWebTokenFilter;

    public SecurityConfig(JsonWebTokenFilter jsonWebTokenFilter) {
        this.jsonWebTokenFilter = jsonWebTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API 不使用 CSRF
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(
                        jsonWebTokenFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        // .authorizeHttpRequests(auth -> auth
        // // 註冊與登入忘記密碼、重設密碼不需要 JWT
        // .requestMatchers(
        // "/auth/register",
        // "/auth/login",
        // "/auth/google",
        // "/auth/password/forgot",
        // "/auth/password/reset"
        // ).permitAll()

        // // 公開會員資料與會員文章
        // .requestMatchers(
        // HttpMethod.GET,
        // "/users/{id}",
        // "/users/{id}/posts"
        // ).permitAll()

        // // 影城
        // .requestMatchers(
        // HttpMethod.GET,
        // "/cinemas/map",
        // "/cinemas/explore"
        // ).permitAll()

        // // 電影
        // .requestMatchers(
        // HttpMethod.GET,
        // "/movies/explore"
        // ).permitAll()

        // // 特典
        // .requestMatchers(
        // HttpMethod.GET,
        // "/benefits",
        // "/benefits/{id}"
        // ).permitAll()

        // // 交換刊登
        // .requestMatchers(
        // HttpMethod.GET,
        // "/exchange-listings",
        // "/exchange-listings/{id}"
        // ).permitAll()

        // // 票券刊登
        // .requestMatchers(
        // HttpMethod.GET,
        // "/ticket-listings",
        // "/ticket-listings/{id}"
        // ).permitAll()

        // // Swagger API 文件
        // .requestMatchers(
        // "/swagger-ui.html",
        // "/swagger-ui/**",
        // "/v3/api-docs",
        // "/v3/api-docs/**"
        // ).permitAll()

        // // 其他 API 都必須通過登入驗證
        // .anyRequest().authenticated()
        // );

        return http.build();
    }
}