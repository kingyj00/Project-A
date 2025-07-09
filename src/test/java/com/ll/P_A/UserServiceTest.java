package com.ll.P_A;

import com.ll.P_A.security.MailService;
import com.ll.P_A.security.UserRepository;
import com.ll.P_A.security.UserService;
import com.ll.P_A.security.UserSignupRequest;
import com.ll.P_A.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.ll.P_A.security.User;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void signup_ThrowsException_WhenUsernameExists() {
        UserSignupRequest request = new UserSignupRequest("existingUser", "password", "nickname", "email@test.com");
        User user = User.builder()
                .username("existingUser")
                .password("encodedPassword")
                .nickname("tester")
                .email("email@test.com")
                .build();
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
    }

    @Test
    void signup_ThrowsException_WhenEmailExists() {
        UserSignupRequest request = new UserSignupRequest("newUser", "password", "nickname", "email@test.com");
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        User user = User.builder()
                .username("someone")
                .password("encoded")
                .nickname("tester")
                .email("email@test.com")
                .build();
        when(userRepository.findByEmail("email@test.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.signup(request));
    }

    @Test
    void signup_Successful_WhenValidRequest() {
        UserSignupRequest request = new UserSignupRequest("newUser", "password", "nickname", "new@test.com");
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        userService.signup(request);

        verify(userRepository).save(any(User.class));
        verify(mailService).sendVerificationEmail(any(User.class));
    }
}