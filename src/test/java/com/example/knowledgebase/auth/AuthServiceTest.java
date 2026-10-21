package com.example.knowledgebase.auth;

import com.example.knowledgebase.common.BadRequestException;
import com.example.knowledgebase.security.JwtService;
import com.example.knowledgebase.user.User;
import com.example.knowledgebase.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Registration should hash password and persist user")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("test@example.com", "secret123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed_secret");

        UUID generatedId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            return u;
        });

        UserResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(generatedId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Registration should reject duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "secret123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login should return JWT token when credentials match")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("test@example.com", "secret123");
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "hashed_secret");
        user.setId(userId);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed_secret")).thenReturn(true);
        when(jwtService.generateToken(userId, "test@example.com")).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400, response.getExpiresInSeconds());
    }

    @Test
    @DisplayName("Login should throw BadCredentialsException on password mismatch")
    void testLoginInvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");
        User user = new User("test@example.com", "hashed_secret");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed_secret")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any(), any());
    }
}
