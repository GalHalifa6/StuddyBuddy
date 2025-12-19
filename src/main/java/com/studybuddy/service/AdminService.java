package com.studybuddy.service;

import com.studybuddy.model.AdminAuditLog;
import com.studybuddy.model.Role;
import com.studybuddy.model.User;
import com.studybuddy.repository.AdminAuditLogRepository;
import com.studybuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin Service
 * Handles admin operations with safety checks and audit logging
 */
@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    /**
     * Get current admin user from security context
     */
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
    }

    /**
     * Check if user is trying to modify themselves (prevent self-lockout)
     */
    private void checkSelfModification(Long targetUserId, String action) {
        User currentAdmin = getCurrentAdmin();
        if (currentAdmin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot " + action + " your own account");
        }
    }

    /**
     * Check if trying to modify last admin account
     */
    private void checkLastAdmin(Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (targetUser.getRole() == Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && !u.getIsDeleted())
                    .count();
            
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot modify the last admin account");
            }
        }
    }

    /**
     * Create audit log entry
     */
    private void logAction(String actionType, String targetType, Long targetId, String reason, Map<String, Object> metadata) {
        User admin = getCurrentAdmin();
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(admin.getId());
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setReason(reason);
        
        if (metadata != null && !metadata.isEmpty()) {
            // Simple JSON-like string (for MVP, can use proper JSON library later)
            log.setMetadata(metadata.toString());
        }
        
        auditLogRepository.save(log);
    }

    /**
     * Suspend a user
     */
    @Transactional
    public User suspendUser(Long userId, LocalDateTime suspendedUntil, String reason) {
        checkSelfModification(userId, "suspend");
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setSuspendedUntil(suspendedUntil);
        user.setSuspensionReason(reason);
        user.setIsActive(false); // Disable login while suspended
        
        User savedUser = userRepository.save(user);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("suspendedUntil", suspendedUntil.toString());
        logAction("SUSPEND", "USER", userId, reason, metadata);
        
        return savedUser;
    }

    /**
     * Ban a user
     */
    @Transactional
    public User banUser(Long userId, String reason) {
        checkSelfModification(userId, "ban");
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBannedAt(LocalDateTime.now());
        user.setBanReason(reason);
        user.setIsActive(false);
        
        User savedUser = userRepository.save(user);
        
        logAction("BAN", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Remove suspension from a user
     */
    @Transactional
    public User unsuspendUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
        // Only enable login if not banned
        if (!user.isBanned()) {
            user.setIsActive(true);
        }
        
        User savedUser = userRepository.save(user);
        
        logAction("UNSUSPEND", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Unban a user
     */
    @Transactional
    public User unbanUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBannedAt(null);
        user.setBanReason(null);
        // Only enable login if not suspended
        if (!user.isSuspended()) {
            user.setIsActive(true);
        }
        
        User savedUser = userRepository.save(user);
        
        logAction("UNBAN", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Soft delete a user
     */
    @Transactional
    public User softDeleteUser(Long userId, String reason) {
        checkSelfModification(userId, "delete");
        checkLastAdmin(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        
        User savedUser = userRepository.save(user);
        
        logAction("SOFT_DELETE", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Restore a soft-deleted user
     */
    @Transactional
    public User restoreUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsDeleted()) {
            throw new IllegalArgumentException("User is not deleted");
        }
        
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        // Only enable login if not banned or suspended
        if (!user.isBanned() && !user.isSuspended()) {
            user.setIsActive(true);
        }
        
        User savedUser = userRepository.save(user);
        
        logAction("RESTORE", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Permanently delete a user (only if already soft deleted)
     */
    @Transactional
    public void permanentDeleteUser(Long userId, String reason) {
        checkSelfModification(userId, "permanently delete");
        checkLastAdmin(userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getIsDeleted()) {
            throw new IllegalArgumentException("User must be soft deleted before permanent deletion");
        }
        
        // Check if soft deleted more than 30 days ago
        if (user.getDeletedAt() != null && user.getDeletedAt().isAfter(LocalDateTime.now().minusDays(30))) {
            throw new IllegalArgumentException("Cannot permanently delete user until 30 days after soft deletion");
        }
        
        logAction("PERMANENT_DELETE", "USER", userId, reason, null);
        userRepository.delete(user);
    }

    /**
     * Update user role with safety checks
     */
    @Transactional
    public User updateUserRole(Long userId, Role newRole, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent removing own admin role
        User currentAdmin = getCurrentAdmin();
        if (currentAdmin.getId().equals(userId) && user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            throw new IllegalArgumentException("Cannot remove your own admin role");
        }
        
        // Check if trying to modify last admin
        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            checkLastAdmin(userId);
        }
        
        Role oldRole = user.getRole();
        user.setRole(newRole);
        User savedUser = userRepository.save(user);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("oldRole", oldRole.name());
        metadata.put("newRole", newRole.name());
        logAction("ROLE_CHANGE", "USER", userId, reason, metadata);
        
        return savedUser;
    }

    /**
     * Disable login without suspension/ban
     */
    @Transactional
    public User disableLogin(Long userId, String reason) {
        checkSelfModification(userId, "disable login for");
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsActive(false);
        User savedUser = userRepository.save(user);
        
        logAction("DISABLE_LOGIN", "USER", userId, reason, null);
        
        return savedUser;
    }

    /**
     * Enable login
     */
    @Transactional
    public User enableLogin(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Don't enable if banned or suspended
        if (user.isBanned()) {
            throw new IllegalArgumentException("Cannot enable login for banned user. Unban first.");
        }
        if (user.isSuspended()) {
            throw new IllegalArgumentException("Cannot enable login for suspended user. Wait for suspension to expire or remove suspension.");
        }
        
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        
        logAction("ENABLE_LOGIN", "USER", userId, reason, null);
        
        return savedUser;
    }
}

