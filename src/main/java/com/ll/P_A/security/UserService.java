package com.ll.P_A.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public void signup(UserSignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .enabled(false)
                .build();

        user.generateVerificationToken();
        userRepository.save(user);
        mailService.sendVerificationEmail(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("이메일 인증을 완료해주세요.");
        }

        return user;
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));

        if (user.isTokenExpired()) {
            throw new IllegalStateException("인증 토큰이 만료되었습니다. 다시 인증을 요청해주세요.");
        }

        user.verifyEmail();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    public UserProfileResponse getMyProfile(Long id) {
        User user = findById(id);
        return new UserProfileResponse(user.getUsername(), user.getEmail(), user.isEmailVerified());
    }

    public List<UserSummary> findAllUsers() {
        return userRepository.findAllUserSummaries();
    }

    @Transactional
    public void updateUser(UserUpdateRequest request, Long id) {
        User user = findById(id);

        // 비밀번호 변경 시 현재 비밀번호 확인
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // 이메일 변경 시 인증 다시 받도록
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            user.setEnabled(false);
            user.generateVerificationToken();
            mailService.sendVerificationEmail(user);
        }

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            user.setNickname(request.getNickname());
        }
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 사용자가 존재하지 않습니다.");
        }

        userRepository.deleteById(id);
    }
}