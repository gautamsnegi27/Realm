package com.gn.reminder.userservice.auth.service;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.LoginRequest;
import com.gn.reminder.userservice.auth.dto.SignupRequest;
import com.gn.reminder.userservice.auth.util.JwtUtil;
import com.gn.reminder.userservice.shared.exception.UserNotFoundException;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.repository.UserRepo;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepo userRepo;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  @Value("${jwt.expiration}")
  private Long jwtExpiration;

  @Transactional
  public AuthResponse signup(SignupRequest request) {
    log.info("Processing signup request for username: {}", request.getUsername());

    // Check if username already exists
    if (userRepo.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username already exists: " + request.getUsername());
    }

    // Check if email already exists
    if (userRepo.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already exists: " + request.getEmail());
    }

    // Create new user
    var user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .isVerified(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    user = userRepo.save(user);
    log.info("User created successfully with ID: {}", user.getId());

    // Generate JWT token
    var token = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getId());

    return new AuthResponse(
            token,
            jwtExpiration,
            user.getUsername(),
            user.getEmail(),
            user.getId()
    );
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    log.info("Processing login request for: {}", request.getUsernameOrEmail());

    // Find user by username or email
    var user = userRepo.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found: " + request.getUsernameOrEmail()));

    // Verify password
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      log.warn("Failed login attempt for user: {}", request.getUsernameOrEmail());
      throw new BadCredentialsException("Invalid credentials");
    }

    // Update last login time
    var updatedUser = user.toBuilder()
            .lastLoginAt(Instant.now())
            .build();
    userRepo.save(updatedUser);

    log.info("User logged in successfully: {}", user.getUsername());

    // Generate JWT token
    var token = jwtUtil.generateToken(user.getUsername(), user.getEmail(), user.getId());

    return new AuthResponse(
            token,
            jwtExpiration,
            user.getUsername(),
            user.getEmail(),
            user.getId()
    );
  }

  public boolean validateToken(String token) {
    return jwtUtil.validateToken(token);
  }

  public String extractUsername(String token) {
    return jwtUtil.extractUsername(token);
  }
}

