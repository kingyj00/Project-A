package com.ll.P_A.security;

import com.ll.P_A.security.jwt.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
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

    // 로그인 - Access, Refresh Token 반환
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("이메일 인증을 완료해주세요.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        return new LoginResponse(accessToken, refreshToken);
    }

    // 이메일 인증 처리
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));

        if (user.isTokenExpired()) {
            throw new IllegalStateException("인증 토큰이 만료되었습니다. 다시 인증을 요청해주세요.");
        }

        user.verifyEmail();
    }

    // 사용자 조회 by ID
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    // 사용자 조회 by Username
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    // 내 정보 조회 (프로필)
    public UserProfileResponse getMyProfile(Long id) {
        User user = findById(id);
        return new UserProfileResponse(user.getUsername(), user.getEmail(), user.isEmailVerified());
    }

    // 관리자 - 전체 사용자 목록
    public List<UserSummary> findAllUsers() {
        return userRepository.findAllUserSummaries();
    }

    // 사용자 정보 수정
    @Transactional
    public void updateUser(UserUpdateRequest request, Long id) {
        User user = findById(id);

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            user.setEnabled(false);
            user.generateVerificationToken();
            mailService.sendVerificationEmail(user);
        }

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            user.setNickname(request.getNickname());
        }
    }

    // 사용자 삭제
    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 사용자가 존재하지 않습니다.");
        }

        userRepository.deleteById(id);
    }
}