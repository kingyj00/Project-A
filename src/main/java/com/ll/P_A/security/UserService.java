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
    private final RefreshTokenService refreshTokenService;

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
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        refreshTokenService.save(user.getUsername(), refreshToken); // Redis에 저장
        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse reissueToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않는 접근 방식입니다.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String savedToken = refreshTokenService.get(username);

        if (!refreshToken.equals(savedToken)) {
            throw new IllegalArgumentException("이미 만료되었거나 일치하지 않는 토큰입니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        refreshTokenService.save(username, newRefreshToken); // Redis 갱신

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String username) {
        refreshTokenService.delete(username); // Redis에서 삭제
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 접근 방식입니다."));

        if (user.isTokenExpired()) {
            throw new IllegalStateException("유효하지 않는 접근 방식입니다.");
        }

        user.verifyEmail();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
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

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
            user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.changeEmail(request.getEmail());
            mailService.sendVerificationEmail(user);
        }

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            user.changeNickname(request.getNickname());
        }

        userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 계정입니다.");
        }
        userRepository.deleteById(id);
    }
}