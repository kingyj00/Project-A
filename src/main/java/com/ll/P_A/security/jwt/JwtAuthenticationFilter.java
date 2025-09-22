package com.ll.P_A.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** JWT 검증을 건너뛸(permitAll/문서/H2/정적 리소스 등) 경로들 */
    private static final String[] SKIP_PATHS = {
            "/", "/error", "/favicon.ico", "/actuator/health",

            // Auth 공개 엔드포인트 (로그인/회원가입/재발급 등)
            "/api/auth/**",

            // 공개 조회 API(예: 게시글 목록/상세) — 프로젝트 정책 따라 조정
            "/api/posts/**",

            // Swagger / OpenAPI
            "/swagger-ui.html", "/swagger-ui/**",
            "/v3/api-docs", "/v3/api-docs/**", "/api-docs/**",
            "/swagger-resources", "/swagger-resources/**",
            "/webjars/**",

            // H2 콘솔
            "/h2-console/**",

            // 정적 리소스
            "/css/**", "/js/**", "/images/**",
            "**/*.css", "**/*.js", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.gif", "**/*.svg"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // CORS preflight(OPTIONS)은 필터 건너뜀
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

        String path = request.getRequestURI();
        for (String pattern : SKIP_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true; // 화이트리스트 경로는 JWT 검사 스킵
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 이미 인증되어 있으면 통과
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);

            // 토큰이 없거나 빈 값이면 — 익명으로 계속 진행 (여기서 401/에러 절대 던지지 않음)
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰이 있으면: 유효성 검사만 하고, 유효할 때만 인증 세팅
            if (jwtTokenProvider.validateToken(token)) {
                // (선택 정책) 보호 API에서는 Access Token만 허용
                String typ = jwtTokenProvider.getTokenType(token);
                if (!"access".equals(typ)) {
                    // 타입이 access가 아니면 인증 세팅 없이 익명으로 통과
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtTokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 유효하지 않은 토큰 — 익명으로 통과 (여기서 401/에러 X)
            }
        } catch (ExpiredJwtException e) {
            // 만료 토큰 — 익명으로 통과
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            // 예외 발생 — 컨텍스트 비우고 익명으로 통과 (스웨거 문서 생성 방해 금지)
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) return null;

        String value = header.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("bearer")) {
            String[] parts = value.split("\\s+");
            if (parts.length >= 2) {
                String token = parts[1].trim();
                if (StringUtils.hasText(token) && !"null".equalsIgnoreCase(token)) {
                    return token;
                }
            }
        }
        return null;
    }

    // (참고) 이제는 필터에서 직접 401을 쓰지 않으므로, 아래 메서드는 사용 안 함.
    @SuppressWarnings("unused")
    private void writeUnauthorizedIfNotCommitted(HttpServletResponse response, String body) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("WWW-Authenticate", "Bearer realm=\"api\", error=\"unauthorized\"");
        response.getWriter().write(body);
    }
}