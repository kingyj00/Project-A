package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetailsService;
import com.ll.P_A.security.jwt.JwtAuthenticationFilter;
import com.ll.P_A.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; // JWT 제공자
    private final CustomUserDetailsService customUserDetailsService; // 사용자 조회
    private final Environment environment; // 프로필/프로퍼티 확인용

    // Swagger 경로 화이트리스트
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", //yaml 경로 포함
            "/api-docs/**",
            "/swagger-resources", "/swagger-resources/**",
            "/webjars/**"
    };

    // H2 콘솔 경로
    private static final String[] H2_WHITELIST = {
            "/h2-console/**"
    };

    // 정적/공용 경로
    private static final String[] PUBLIC_WHITELIST = {
            "/", "/error", "/actuator/health", "/favicon.ico",
            "/css/**", "/js/**", "/images/**"
    };

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private boolean isLocalLike() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) return true;
        for (String p : profiles) {
            if ("dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p)) return true;
        }
        return false;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean h2Enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, false);

        http
                // H2 콘솔 iframe 허용
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                // JWT 기반: CSRF 비활성화 (Swagger/H2는 예외 등록)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/swagger-resources/**"),
                                new AntPathRequestMatcher("/webjars/**"),
                                new AntPathRequestMatcher("/swagger-ui.html")
                        )
                        .disable()
                )

                // 세션 X / 폼로그인 X / Basic X
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(hb -> hb.disable())

                // CORS 허용 (아래 Bean과 연동)
                .cors(cors -> {})

                // 인증/인가 에러를 401/403으로 명확히
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authEx) -> {
                            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, accessEx) -> {
                            if (!response.isCommitted()) response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                )

                // 인가 규칙
                .authorizeHttpRequests(auth -> {
                    // 프리플라이트
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // 공개 API (정확히 필요한 것만 허용)
                    auth.requestMatchers(
                            "/api/auth/signup",
                            "/api/auth/login",
                            "/api/auth/reissue",
                            "/api/auth/verify-email"
                    ).permitAll();

                    // 예: 게시글 조회 공개
                    auth.requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll();

                    // 정적/공용 리소스
                    auth.requestMatchers(PUBLIC_WHITELIST).permitAll();

                    // Swagger 문서/리소스는 항상 허용
                    auth.requestMatchers(SWAGGER_WHITELIST).permitAll();

                    // H2 콘솔은 설정 시에만 허용
                    if (h2Enabled) auth.requestMatchers(H2_WHITELIST).permitAll();

                    // 나머지 전부 인증 필요
                    auth.anyRequest().authenticated();
                });

        // JWT 필터 삽입
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    // CORS 정책
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));  // 운영에서는 구체 도메인으로 제한 권장
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}