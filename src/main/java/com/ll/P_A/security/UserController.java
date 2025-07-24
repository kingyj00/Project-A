package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public String signup(@RequestBody UserSignupRequest request) {
        userService.signup(request);
        return "가입 완료! 이메일을 확인해주세요.";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        return "이메일 인증 완료!";
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.logout(loginUser.getUsername());
        return "로그아웃 완료되었습니다.";
    }

    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshToken) {
        return userService.reissueToken(refreshToken);
    }

    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User admin = loginUser.getUser();
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        // 관리자 권한으로 삭제하므로 currentUserId도 관리자 ID로 넘김
        userService.deleteById(id, admin.getId());
        return "사용자 삭제 완료";
    }

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

    @GetMapping("/me")
    public UserProfileResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return userService.getMyProfile(loginUser.getUser().getId());
    }

    @PutMapping("/me")
    public String updateMyInfo(@RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Long currentUserId = loginUser.getUser().getId();
        userService.updateUser(request, currentUserId, currentUserId); // 그인 ID를 검증용으로 전달
        return "회원정보 수정 완료";
    }

    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Long currentUserId = loginUser.getUser().getId();
        userService.deleteById(currentUserId, currentUserId); //로그인 ID를 검증용으로 전달
        return "회원 탈퇴가 완료되었습니다.";
    }
}