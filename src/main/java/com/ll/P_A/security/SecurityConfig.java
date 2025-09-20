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

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider; // JWT 제공자
    private final CustomUserDetailsService customUserDetailsService; // 사용자 조회
    private final Environment environment; // 프로필/프로퍼티 확인용

    // Swagger 경로 화이트리스트(전체)
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/api-docs/**",
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
        return new BCryptPasswordEncoder(); // 비밀번호 인코더
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // 인증 매니저
    }

    // dev/test 또는 활성 프로필이 없는(default) 경우 로컬처럼 간주
    private boolean isLocalLike() {
        String[] profiles = environment.getActiveProfiles(); // 활성 프로필
        if (profiles == null || profiles.length == 0) return true; // 프로필 비어있으면 로컬처럼
        for (String p : profiles) {
            if ("dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p)) return true; // dev/test는 로컬처럼
        }
        return false; // 그 외(prod 등)
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean localLike = isLocalLike(); // 로컬 모드 여부
        boolean h2Enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class, false); // H2 콘솔 on/off

        http
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)) // H2 콘솔 iframe 허용

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers( // Swagger/H2는 CSRF 예외
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/swagger-resources/**"),
                                new AntPathRequestMatcher("/webjars/**"),
                                new AntPathRequestMatcher("/swagger-ui.html")
                        )
                        .disable() // JWT 사용 시 CSRF 비활성
                )

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .formLogin(fl -> fl.disable()) // 폼 로그인 비활성
                .httpBasic(hb -> hb.disable()) // 기본 인증 비활성
                .cors(cors -> {}) // CORS 기본 허용

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll(); // 프리플라이트 허용

                    auth.requestMatchers("/api/auth/**").permitAll(); // 로그인/회원가입/토큰 재발급 등 공개
                    auth.requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll(); // 게시글 조회 공개
                    auth.requestMatchers(PUBLIC_WHITELIST).permitAll(); // 정적/공용

                    // Swagger는 로컬처럼 동작할 때만 전체 허용
                    if (localLike) auth.requestMatchers(SWAGGER_WHITELIST).permitAll();

                    // H2 콘솔은 spring.h2.console.enabled=true 일 때만 허용(프로필 무관)
                    if (h2Enabled) auth.requestMatchers(H2_WHITELIST).permitAll();

                    auth.anyRequest().authenticated(); // 나머지는 인증 필요
                });

        // JWT 필터 삽입(UsernamePasswordAuthenticationFilter 앞)
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}