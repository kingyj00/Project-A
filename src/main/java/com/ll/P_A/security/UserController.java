package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService; // Refresh 토큰 로직 호출용

    /* ---------- 공통 유틸 ---------- */

    private String extractBearer(String header) {
        if (!StringUtils.hasText(header)) return null;
        if (header.startsWith("Bearer ")) return header.substring(7);
        return header;
    }

    /* ---------- API ---------- */

    @PostMapping("/signup")
    public String signup(@Valid @RequestBody UserSignupRequest request) {
        userService.signup(request);
        return "가입 완료! 이메일을 확인해주세요.";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return "이메일 인증 완료!";
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal CustomUserDetails loginUser) {
        userService.logout(loginUser.getUsername());
        return "로그아웃 완료되었습니다.";
    }

    // Refresh 로테이션 적용
    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshHeader) {
        String refreshToken = extractBearer(refreshHeader);

        // 1. 토큰 메타 확인
        RefreshTokenMeta meta = refreshTokenService.findByToken(refreshToken);
        if (meta == null || meta.isRevoked() || meta.isExpired()) {
            // 재사용 공격 or 만료
            if (meta != null) {
                refreshTokenService.revokeFamily(meta.getFamilyId()); // 묶음 전체 차단
            }
            throw new RuntimeException("재로그인이 필요합니다.");
        }

        // 2. 이전 토큰 폐기
        refreshTokenService.revoke(meta.getTokenId());

        // 3. 새 Access + 새 Refresh 발급
        return userService.reissueTokenWithRotation(meta.getUserId(), meta.getFamilyId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails loginUser) {
        User admin = loginUser.getUser();
        userService.deleteById(id, admin.getId());
        return "사용자 삭제 완료";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public List<UserSummary> listUsers() {
        return userService.findAllUsers();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public UserProfileResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails loginUser) {
        return userService.getMyProfile(loginUser.getUser().getId());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public String updateMyInfo(@Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.updateUser(request, currentUserId, currentUserId);
        return "회원정보 수정 완료";
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.deleteById(currentUserId, currentUserId);
        return "회원 탈퇴가 완료되었습니다.";
    }
}