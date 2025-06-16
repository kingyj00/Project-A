package com.ll.P_A.security;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
    public String login(@RequestBody LoginRequest request, HttpSession session) {
        User user = userService.login(request);
        session.setAttribute("userId", user.getId());
        return "로그인 성공";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "로그아웃 성공";
    }

    @DeleteMapping("/admin/users/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        Long requesterId = (Long) session.getAttribute("userId");
        if (requesterId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User admin = userService.findById(requesterId);
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        userService.deleteById(id);
        return "사용자 삭제 완료";
    }

    @GetMapping("/admin/users")
    public List<UserSummary> listUsers(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        User admin = userService.findById(userId);
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        return userService.findAllUsers();
    }

    @GetMapping("/me")
    public UserProfileResponse getMyInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return userService.getMyProfile(userId);
    }

    @PutMapping("/me")
    public String updateMyInfo(@RequestBody UserUpdateRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.updateUser(request, userId);
        return "회원정보 수정 완료";
    }

    @DeleteMapping("/me")
    public String deleteMyAccount(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        userService.deleteById(userId);
        session.invalidate();
        return "회원 탈퇴가 완료되었습니다.";
    }
}