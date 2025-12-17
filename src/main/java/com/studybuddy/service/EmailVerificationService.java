package com.studybuddy.service;

import com.studybuddy.model.EmailVerificationToken;
import com.studybuddy.model.User;
import com.studybuddy.repository.EmailVerificationTokenRepository;
import com.studybuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for managing email verification tokens
 */
@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int TOKEN_LENGTH = 32; // bytes
    private static final int TOKEN_VALIDITY_HOURS = 24;

    /**
     * Generates and sends a verification email to the user
     * @param user The user to send verification email to
     * @return The raw token (not hashed) to be sent in email
     */
    @Transactional
    public String createAndSendVerificationToken(User user) {
        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate a secure random token
        String rawToken = generateSecureToken();
        String hashedToken = passwordEncoder.encode(rawToken);

        // Create token entity
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hashedToken);
        token.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        token.setUsed(false);

        tokenRepository.save(token);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), rawToken);

        return rawToken;
    }

    /**
     * Verifies a token and marks the user's email as verified
     * @param rawToken The raw token from the verification link
     * @return true if verification was successful
     */
    @Transactional
    public boolean verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return false;
        }

        // Find all tokens and check against the raw token
        // Since tokens are hashed, we need to check each one
        for (EmailVerificationToken token : tokenRepository.findAll()) {
            if (passwordEncoder.matches(rawToken, token.getTokenHash())) {
                // Check if token is valid
                if (token.getUsed()) {
                    return false; // Token already used
                }
                if (token.isExpired()) {
                    return false; // Token expired
                }

                // Mark token as used
                token.setUsed(true);
                tokenRepository.save(token);

                // Mark user's email as verified
                User user = token.getUser();
                user.setEmailVerified(true);
                userRepository.save(user);

                return true;
            }
        }

        return false; // Token not found
    }

    /**
     * Checks if a user has a pending (valid) verification token
     */
    public boolean hasPendingVerificationToken(User user) {
        return tokenRepository.findByUser(user).stream()
                .anyMatch(token -> !token.getUsed() && !token.isExpired());
    }

    /**
     * Generates a secure random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Cleans up expired tokens (can be called by a scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}

