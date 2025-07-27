package com.ll.P_A;

import com.ll.P_A.global.exception.AuthorizationValidator;
import com.ll.P_A.security.*;
import com.ll.P_A.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthorizationValidator authValidator;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_ThrowsException_WhenUsernameExists() {
        UserSignupRequest request = new UserSignupRequest("existingUser", "password", "nickname", "email@test.com");
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 아이디입니다.");
    }

    @Test
    void signup_ThrowsException_WhenEmailExists() {
        UserSignupRequest request = new UserSignupRequest("user", "password", "nickname", "email@test.com");
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("email@test.com")).thenReturn(Optional.of(mock(User.class)));

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다.");
    }

    @Test
    void signup_Success_WhenValidRequest() {
        UserSignupRequest request = new UserSignupRequest("newUser", "password", "nickname", "new@test.com");
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPw");

        assertDoesNotThrow(() -> userService.signup(request));
        verify(userRepository).save(any(User.class));
        verify(mailService).sendVerificationEmail(any(User.class));
    }

    @Test
    void updateUser_Successful_WhenPasswordMatches() {
        // given
        Long userId = 1L;
        String rawPassword = "currentPw";
        String encodedPassword = "encodedPw";

        User user = User.builder()
                .id(userId)
                .username("tester")
                .email("old@test.com")
                .nickname("oldNick")
                .build();

        // 빌더로는 password가 안 들어갈 수 있으므로 수동 설정
        user.updatePassword(encodedPassword);

        UserUpdateRequest request = new UserUpdateRequest(
                rawPassword,          // 현재 비밀번호
                "newPw",              // 새 비밀번호
                "new@test.com",
                "newNick"
        );

        // mocking
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(rawPassword), eq(encodedPassword))).thenReturn(true);
        when(passwordEncoder.encode("newPw")).thenReturn("encodedNewPw");

        // when & then
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            String encoded = invocation.getArgument(1);
            System.out.println("matches 호출됨: raw=" + raw + ", encoded=" + encoded);
            return raw.equals("currentPw") && encoded.equals("encodedPw");
        });
    }

    @Test
    void deleteById_Successful_WhenAuthorized() {
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteById(userId, userId);

        verify(authValidator).validateAuthor(user, userId);
        verify(userRepository).delete(user);
    }
}