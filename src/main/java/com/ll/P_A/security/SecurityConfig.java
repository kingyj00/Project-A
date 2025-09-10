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

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final Environment environment;

    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/api-docs/**"
    };

    private static final String[] H2_WHITELIST = {
            "/h2-console/**"
    };

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

    /** dev 또는 test 프로필이면 true */
    private boolean isDevOrTest() {
        for (String p : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p)) return true;
        }
        return false;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isDevOrTest = isDevOrTest();

        http
                /* H2 콘솔이 iframe으로 뜨기 때문에 sameOrigin 필요 */
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

                /* JWT 기반이므로 CSRF 비활성화.
                   단, Swagger/H2는 CSRF 예외로 빼주면 콘솔/문서 POST 요청에서도 403 방지됨 */
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**")
                        )
                        .disable()
                )

                /* 세션 비사용(Stateless) + form/basic 로그인 비활성 */
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(hb -> hb.disable())

                /* CORS 기본 허용 (필요 시 WebMvcConfigurer로 상세 설정) */
                .cors(cors -> {})

                /* 엔드포인트 인가 규칙 */
                .authorizeHttpRequests(auth -> {
                    // 프리플라이트(OPTIONS) 허용
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // 공개 API
                    auth.requestMatchers("/api/auth/**").permitAll(); // 로그인/회원가입/재발급 등
                    auth.requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll(); // 게시글 조회 공개

                    // 공용 공개 리소스
                    auth.requestMatchers(PUBLIC_WHITELIST).permitAll();

                    // dev/test 프로필에서는 Swagger/H2 콘솔 허용
                    if (isDevOrTest) {
                        auth.requestMatchers(SWAGGER_WHITELIST).permitAll();
                        auth.requestMatchers(H2_WHITELIST).permitAll();
                    }

                    // 나머지는 인증 필요
                    auth.anyRequest().authenticated();
                });

        // JWT 인증 필터 삽입 (UsernamePasswordAuthenticationFilter 앞)
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}