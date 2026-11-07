package com.moyuan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.common.R;
import com.moyuan.entity.SecurityEvent;
import com.moyuan.security.JwtAuthenticationFilter;
import com.moyuan.service.SecurityEventService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final SecurityEventService securityEventService;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/static/**",
                    "/favicon.ico",
                    "/doc.html",
                    "/webjars/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/api/auth/**",
                    "/api/poets",
                    "/api/poets/**",
                    "/api/dynasties",
                    "/api/dynasties/**",
                    "/api/categories",
                    "/api/categories/**",
                    "/api/forum/posts",
                    "/api/forum/posts/**",
                    "/api/forum/comments/**",
                    "/api/vision",
                    "/api/vision/**",
                    "/api/system-knowledge",
                    "/api/search",
                    "/api/search/**",
                    "/api/external-poetry",
                    "/api/external-poetry/**",
                    "/api/game",
                    "/api/game/**",
                    "/api/poems/fix-external-poems",
                    "/uploads/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/poems", "/api/poems/{id:[0-9]+}", "/api/poems/{id:[0-9]+}/ratings", "/api/poems/{id:[0-9]+}/ratings/ai").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/poems/modern/page", "/api/poems/random", "/api/poems/daily").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/poet-featured", "/api/poet-featured/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/home-navigation", "/api/home-navigation/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/rhyme/**").permitAll()
                // 模型策略推荐的上/下线模型信息与可用策略：公开可选 JWT；/api/recommend 本体保持登录
                .requestMatchers(HttpMethod.GET, "/api/recommend/models").permitAll()
                // 管理端指标屏的实时统计仅管理员可见
                .requestMatchers(HttpMethod.GET, "/api/recommend/stats").hasRole("ADMIN")
                // 审核流水线：看板与工单流转允许审核员使用；管理员可协审
                .requestMatchers(HttpMethod.GET,
                        "/api/admin/audit-pipeline/board",
                        "/api/admin/audit-pipeline/records",
                        "/api/admin/audit-pipeline/reviewers").hasAnyRole("ADMIN", "REVIEWER")
                .requestMatchers(HttpMethod.POST,
                        "/api/admin/audit-pipeline/*/claim",
                        "/api/admin/audit-pipeline/*/transfer",
                        "/api/admin/audit-pipeline/*/return").hasAnyRole("ADMIN", "REVIEWER")
                // 建单、分派、规则配置、审核员团队管理仅管理员可用
                .requestMatchers("/api/admin/audit-pipeline/**").hasRole("ADMIN")
                // 审核员需要读待审列表并提交审核结果，仅放行审核所需的这几个端点，其余管理接口仍限 ADMIN
                .requestMatchers(HttpMethod.GET,
                        "/api/admin/poems",
                        "/api/admin/comments",
                        "/api/admin/ai-contents",
                        "/api/admin/poet-profiles",
                        "/api/admin/poet-drafts",
                        "/api/admin/poet-suggestions",
                        "/api/admin/forum-posts",
                        "/api/admin/forum-posts/*",
                        "/api/admin/audit/stats").hasAnyRole("ADMIN", "REVIEWER")
                .requestMatchers(HttpMethod.PUT,
                        "/api/admin/poems/*/audit",
                        "/api/admin/comments/*/audit",
                        "/api/admin/poet-profiles/*/verify",
                        "/api/admin/ai-contents/*/approve",
                        "/api/admin/ai-contents/*/reject",
                        "/api/admin/poet-drafts/*/review",
                        "/api/admin/forum-posts/*/status",
                        "/api/admin/poet-suggestions/*/review").hasAnyRole("ADMIN", "REVIEWER")
                // 举报管理：管理员与审核员都可以查看并处理
                .requestMatchers("/api/admin/reports/**").hasAnyRole("ADMIN", "REVIEWER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(objectMapper.writeValueAsString(R.error("401", "未登录或Token已过期")));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // 越权访问由过滤器直接写出响应，不经过 MVC，只能在这里记安全事件
                    securityEventService.recordFromRequest(SecurityEvent.TYPE_FORBIDDEN,
                            request.getMethod() + " " + request.getRequestURI(), request, null);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(objectMapper.writeValueAsString(R.error("403", "没有操作权限")));
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
