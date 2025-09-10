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

    /** JWT 검증을 건너뛸(permitAll/개발편의) 경로들 */
    private static final String[] SKIP_PATHS = {
            "/",
            "/error",
            "/favicon.ico",
            "/actuator/health",

            // Auth 공개 엔드포인트 (로그인/회원가입/재발급 등)
            "/api/auth/**",

            // 공개 조회 API(예: 게시글 목록/상세)
            "/api/posts/**",

            // 정적/문서/H2 콘솔
            "/css/**", "/js/**", "/images/**",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**",
            "/h2-console/**"
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

        // 이미 인증된 경우 재인증 방지
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String username = jwtTokenProvider.getUsernameFromToken(token);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // 토큰 포맷은 있었으나 유효하지 않음 → 401
                        writeUnauthorizedIfNotCommitted(response, "{\"error\":\"Invalid Token\"}");
                        return;
                    }
                } catch (ExpiredJwtException e) {
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedIfNotCommitted(response, "{\"error\":\"AccessToken Expired\"}");
                    return;
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedIfNotCommitted(response, "{\"error\":\"Invalid Token\"}");
                    return;
                }
            }
            // 토큰이 없으면(헤더 없음) -> 퍼밋이 아닌 경로는 이후 Security에서 401/403 처리
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) return null;

        String value = header.trim();
        // e.g., "Bearer abc.def.ghi" / "bearer   abc..."
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

    private void writeUnauthorizedIfNotCommitted(HttpServletResponse response, String body) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        // 클라이언트 디버깅 편의
        response.setHeader("WWW-Authenticate", "Bearer realm=\"api\", error=\"unauthorized\"");
        response.getWriter().write(body);
    }
}