package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Auth", description = "인증/회원 관리 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* ---------- 공통 유틸 ---------- */
    private String extractBearer(String header) {
        if (!StringUtils.hasText(header)) return null;
        if (header.startsWith("Bearer ")) return header.substring(7);
        return header;
    }

    /* ---------- API ---------- */

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다.")
    @PostMapping("/signup")
    public String signup(@Valid @RequestBody UserSignupRequest request) {
        userService.signup(request);
        return "가입 완료! 이메일을 확인해주세요.";
    }

    @Operation(summary = "이메일 인증", description = "이메일로 받은 토큰으로 계정을 활성화합니다.")
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return "이메일 인증 완료!";
    }

    @Operation(summary = "로그인", description = "인증 성공 시 Access/Refresh 토큰을 반환합니다.")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @Operation(summary = "로그아웃", description = "로그인 사용자의 모든 세션(Refresh)을 무효화합니다.")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal CustomUserDetails loginUser) {
        userService.logout(loginUser.getUsername());
        return "로그아웃 완료되었습니다.";
    }

    @Operation(summary = "토큰 재발급", description = "유효한 Refresh 토큰으로 새 Access/Refresh 토큰을 발급합니다.")
    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshHeader) {
        String refreshToken = extractBearer(refreshHeader);
        if (!StringUtils.hasText(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh 토큰이 없습니다.");
        }
        // 로테이션/검증/저장은 서비스에서 처리
        return userService.reissueToken(refreshToken);
    }

    @Operation(summary = "사용자 삭제(관리자)", description = "관리자가 특정 사용자를 삭제합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails loginUser) {
        User admin = loginUser.getUser();
        userService.deleteById(id, admin.getId());
        return "사용자 삭제 완료";
    }

    @Operation(summary = "사용자 목록(관리자)", description = "관리자가 사용자 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public List<UserSummary> listUsers() {
        return userService.findAllUsers();
    }

    @Operation(summary = "내 정보 조회", description = "로그인 사용자의 프로필을 조회합니다.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public UserProfileResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails loginUser) {
        return userService.getMyProfile(loginUser.getUser().getId());
    }

    @Operation(summary = "내 정보 수정", description = "로그인 사용자의 닉네임/이메일/비밀번호를 수정합니다.")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public String updateMyInfo(@Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.updateUser(request, currentUserId, currentUserId);
        return "회원정보 수정 완료";
    }

    @Operation(summary = "회원 탈퇴", description = "로그인 사용자의 계정을 삭제합니다.")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.deleteById(currentUserId, currentUserId);
        return "회원 탈퇴가 완료되었습니다.";
    }
}