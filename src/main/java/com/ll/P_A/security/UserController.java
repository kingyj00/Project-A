package com.ll.P_A.security;

import com.ll.P_A.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal CustomUserDetails loginUser) {
        userService.logout(loginUser.getUsername());
        return "로그아웃 완료되었습니다.";
    }

    @PostMapping("/reissue")
    public LoginResponse reissue(@RequestHeader(HttpHeaders.AUTHORIZATION) String refreshToken) {
        return userService.reissueToken(refreshToken);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails loginUser) {
        User admin = loginUser.getUser();
        userService.deleteById(id, admin.getId()); // 관리자 ID로 감사/검증
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
    public String updateMyInfo(@RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.updateUser(request, currentUserId, currentUserId); // 자기자신 검증
        return "회원정보 수정 완료";
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public String deleteMyAccount(@AuthenticationPrincipal CustomUserDetails loginUser) {
        Long currentUserId = loginUser.getUser().getId();
        userService.deleteById(currentUserId, currentUserId); // 자기자신 검증
        return "회원 탈퇴가 완료되었습니다.";
    }
}