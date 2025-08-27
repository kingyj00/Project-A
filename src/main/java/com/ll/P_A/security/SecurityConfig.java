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

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final Environment environment;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // UserService가 이 타입을 직접 의존
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private boolean isDevProfileActive() {
        for (String p : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(p)) return true;
        }
        return false;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isDev = isDevProfileActive();

        http
                // 세션 전면 비사용 + 폼로그인/HTTP Basic 비활성(Stateless 보장)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(hb -> hb.disable())

                // 엔드포인트 접근 정책
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/**").permitAll();            // 로그인/회원가입/재발급 등
                    auth.requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll(); // 게시글 조회는 공개

                    if (isDev) {
                        // 개발 프로파일에서만 Swagger 허용
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })

                // JWT 인증 필터 삽입
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}