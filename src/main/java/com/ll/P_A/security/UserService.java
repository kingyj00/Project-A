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
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();

        user.generateVerificationToken();
        userRepository.save(user);

        //이메일 전송 추가됨
        mailService.sendVerificationEmail(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        if (!user.isEnabled()) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user; // 로그인 성공
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        user.verifyEmail();
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    public List<UserSummary> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserSummary::new)
                .toList();
    }

    public UserProfileResponse getMyProfile(Long userId) {
        User user = findById(userId);
        return new UserProfileResponse(user);
    }

    @Transactional
    public void updateUser(Long id, UserUpdateRequest request) {
        User user = findById(id); // 세션 ID로 본인 찾기

        user.setNickname(request.getNickname());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user); // DB 반영
    }
}

