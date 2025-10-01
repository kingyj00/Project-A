package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

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

    // 로테이션 적용 (현재 Refresh는 ROTATED 표시 → 새 토큰 발급)
    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshHeader) {
        String refreshToken = extractBearer(refreshHeader);
        if (!StringUtils.hasText(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh 토큰이 없습니다.");
        }

        RefreshTokenService.RefreshRecord rec = refreshTokenService.findByToken(refreshToken);
        if (rec == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh 토큰입니다.");
        }
        if (rec.isExpired()) {
            // 만료 → 재로그인 요구 (선택: 폐기)
            refreshTokenService.revokeByHash(rec.getHash());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "만료된 Refresh 토큰입니다. 다시 로그인해주세요.");
        }
        if (!rec.isActive()) {
            // ROTATED/REVOKED 상태로 재사용 시도 → family 전체 차단
            refreshTokenService.revokeFamily(rec.getFamilyId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "재사용이 감지되었습니다. 다시 로그인해주세요.");
        }

        // 정상: 현재 토큰을 ROTATED로 표시(재사용 감지용 보관)
        refreshTokenService.markRotatedByHash(rec.getHash());

        // 새 Access/Refresh 발급은 기존 로직 그대로 사용
        // (UserService.reissueToken(refreshToken) 내부에서 새 Refresh 저장 로직이 없다면, 이후 단계에서 보강)
        return userService.reissueToken(refreshToken);
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