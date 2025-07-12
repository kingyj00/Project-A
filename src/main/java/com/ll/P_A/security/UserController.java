package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public String signup(@RequestBody UserSignupRequest request) {
        userService.signup(request);
        return "가입 완료! 이메일을 확인해주세요.";
    }

    // 이메일 인증
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return "이메일 인증 완료!";
    }

    // 로그인 - Access & Refresh Token 반환
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    // 로그아웃 - Redis에서 Refresh Token 삭제
    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.logout(loginUser.getUsername());
        return "로그아웃 완료되었습니다.";
    }

    // Refresh Token 재발급
    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshToken) {
        return userService.reissueToken(refreshToken);
    }

    // 관리자 전용 - 사용자 삭제
    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User admin = loginUser.getUser();
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        userService.deleteById(id);
        return "사용자 삭제 완료";
    }

    // 관리자 전용 - 전체 사용자 조회
    @GetMapping("/admin/users")
    public List<UserSummary> listUsers(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User admin = loginUser.getUser();
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        return userService.findAllUsers();
    }

    // 내 정보 조회
    @GetMapping("/me")
    public UserProfileResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return userService.getMyProfile(loginUser.getUser().getId());
    }

    // 내 정보 수정
    @PutMapping("/me")
    public String updateMyInfo(@RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.updateUser(request, loginUser.getUser().getId());
        return "회원정보 수정 완료";
    }

    // 내 계정 삭제
    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.deleteById(loginUser.getUser().getId());
        return "회원 탈퇴가 완료되었습니다.";
    }
}