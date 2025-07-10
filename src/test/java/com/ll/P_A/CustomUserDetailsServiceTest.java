package com.ll.P_A;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserRepository;
import com.ll.P_A.security.jwt.CustomUserDetails;
import com.ll.P_A.security.jwt.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_ReturnsCustomUserDetails_WhenUserExists() {
        User user = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .nickname("tester")
                .email("test@example.com")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> {
            CustomUserDetails details = (CustomUserDetails) customUserDetailsService.loadUserByUsername("testuser");
            assertEquals("testuser", details.getUsername());
            assertEquals("encodedPassword", details.getPassword());
        });
    }

    @Test
    void loadUserByUsername_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("unknown");
        });
    }
}
