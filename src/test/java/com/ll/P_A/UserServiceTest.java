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

        User user = User.builder()
                .id(userId)
                .username("tester")
                .password("encodedPw")
                .email("old@test.com")
                .nickname("oldNick")
                .build();

        UserUpdateRequest request = new UserUpdateRequest(
                "currentPw",
                "newPw",
                "new@test.com",
                "newNick"
        );

        // mocking
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPw", "encodedPw")).thenReturn(true);
        when(passwordEncoder.encode("newPw")).thenReturn("encodedNewPw");

        // when & then
        assertDoesNotThrow(() -> userService.updateUser(request, userId, userId));

        verify(authValidator).validateAuthor(user, userId);
        verify(userRepository).save(user);
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