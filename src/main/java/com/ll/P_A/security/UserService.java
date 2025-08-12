package com.ll.P_A.security;

import com.ll.P_A.global.exception.AuthorizationValidator;
import com.ll.P_A.security.jwt.JwtTokenProvider;
import com.ll.P_A.security.jwt.JwtTokenProvider.RefreshPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

    /* ==========
       유틸: 이메일 인증 토큰 해시(SHA-256)
       ========== */
    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

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
                .enabled(false) // 이메일 인증 완료 전까지 비활성
                .build();

        // 안전한 인증 토큰 생성: DB에는 해시/만료 저장, 반환값은 '원문 토큰'
        String rawToken = user.generateVerificationToken();

        userRepository.save(user);
        // 메일 발송은 '원문 토큰'으로 링크 구성
        mailService.sendVerificationEmail(user, rawToken);
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

        // 미인증 계정 로그인 차단(이미 구현되어 있던 정책 유지)
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("이메일 인증을 완료해주세요.");
        }

        // 로그인 성공: 실패 카운트 리셋
        user.resetLoginFailCount();
        userRepository.save(user);

        // Access/Refresh 발급 (리프레시 회전/재사용 감지 구조)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String deviceId = "web"; // 필요 시 클라이언트에서 전달받아 사용
        RefreshPayload rp = jwtTokenProvider.generateRefreshToken(user.getUsername(), deviceId);

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
        // 전달된 username의 모든 기기 세션 종료(전체 무효화)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public void verifyEmail(String tokenRaw) {
        // 수신한 원문 토큰을 해시로 변환해 조회
        String hash = sha256Hex(tokenRaw);

        // 레포지토리는 '해시'로 조회하도록 변경 필요
        User user = userRepository.findByEmailVerificationTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 접근 방식입니다."));

        // 만료 확인 (24시간 등)
        if (user.isVerificationExpired()) {
            throw new IllegalStateException("인증 링크가 만료되었습니다. 인증 메일을 다시 요청해 주세요.");
        }

        // 성공 → 1회용 소진 + 인증 완료
        user.verifyEmail();
        userRepository.save(user);
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
            // 이메일 변경 시: 새 토큰 생성(해시/만료 저장) + 원문 토큰으로 메일 발송
            String raw = user.changeEmail(request.getEmail());
            mailService.sendVerificationEmail(user, raw);
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