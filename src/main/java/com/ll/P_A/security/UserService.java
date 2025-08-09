package com.ll.P_A.security;

import com.ll.P_A.global.exception.AuthorizationValidator;
import com.ll.P_A.security.jwt.JwtTokenProvider;
import com.ll.P_A.security.jwt.JwtTokenProvider.RefreshPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
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
    private final AuthorizationValidator authValidator;

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

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        // 잠금 해제 타이밍 확인
        user.unlockIfTimePassed();

        if (user.isCurrentlyLocked()) {
            long remaining = user.getLockRemainingSeconds();
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            throw new LockedException(String.format("계정이 잠겨 있습니다. %d분 %d초 후 다시 시도해주세요.", minutes, seconds));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.increaseLoginFailCount();
            userRepository.save(user);
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("이메일 인증을 완료해주세요.");
        }

        // 로그인 성공: 실패 카운트 리셋
        user.resetLoginFailCount();
        userRepository.save(user);

        // Access/Refresh 발급
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String deviceId = "web"; // 필요 시 클라이언트에서 전달받아 사용
        RefreshPayload rp = jwtTokenProvider.generateRefreshToken(user.getUsername(), deviceId);

        // refresh 해시 기반 ACTIVE 저장 (회전/감지 대비)
        refreshTokenService.storeActiveToken(
                rp.token(),
                user.getId(),
                rp.jti(),
                deviceId,
                rp.expiresAt().toInstant()
        );

        return new LoginResponse(accessToken, rp.token());
    }

    @Transactional
    public LoginResponse reissueToken(String refreshToken) {
        // 형식/서명 검증 + 타입 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않는 접근 방식입니다.");
        }
        String typ = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equalsIgnoreCase(typ)) {
            throw new IllegalArgumentException("유효하지 않는 접근 방식입니다.");
        }

        // 사용자 로드
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        // 재사용 감지/회전 처리
        RefreshTokenService.RefreshRecord rec = refreshTokenService.findByToken(refreshToken);
        if (rec == null || !rec.isActive()) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new IllegalArgumentException("세션이 만료되었거나 보안상 재로그인이 필요합니다.");
        }

        // 정상 요청 → 기존 토큰 ROTATED 표시
        refreshTokenService.markRotated(rec.getHash());

        // 새 토큰 발급 (기존 deviceId 재사용)
        String deviceId = jwtTokenProvider.getDeviceId(refreshToken);
        if (deviceId == null) deviceId = "web";

        String newAccessToken = jwtTokenProvider.generateAccessToken(username);
        RefreshPayload newRp = jwtTokenProvider.generateRefreshToken(username, deviceId);

        // 새 refresh ACTIVE 저장
        refreshTokenService.storeActiveToken(
                newRp.token(),
                user.getId(),
                newRp.jti(),
                deviceId,
                newRp.expiresAt().toInstant()
        );

        return new LoginResponse(newAccessToken, newRp.token());
    }

    @Transactional
    public void logout(String username) {
        // 기존 시그니처 유지: 전달된 username의 모든 기기 세션 종료
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        refreshTokenService.revokeAllForUser(user.getId());
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
    public void updateUser(UserUpdateRequest request, Long id, Long currentUserId) {
        User user = findById(id);
        authValidator.validateAuthor(user, currentUserId);

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
    public void deleteById(Long id, Long currentUserId) {
        User user = findById(id);
        authValidator.validateAuthor(user, currentUserId);
        userRepository.delete(user);
    }
}