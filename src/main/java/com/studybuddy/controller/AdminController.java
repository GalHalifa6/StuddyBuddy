package com.studybuddy.controller;

import com.studybuddy.dto.AuthDto;
import com.studybuddy.dto.UserAdminDto;
import com.studybuddy.model.Role;
import com.studybuddy.repository.UserRepository;
import com.studybuddy.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Controller
 * Handles admin-only operations
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserAdminDto>> getAllUsers(@RequestParam(required = false) Boolean includeDeleted) {
        // Include deleted users if requested, otherwise filter them out
        List<UserAdminDto> users = userRepository.findAll().stream()
                .filter(user -> includeDeleted != null && includeDeleted || !user.getIsDeleted())
                .map(UserAdminDto::fromUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminDto> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserAdminDto::fromUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        try {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());
            adminService.updateUserRole(id, newRole, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse(
                    "User role updated to " + newRole.getDisplayName(), true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        try {
            if (request.isActive()) {
                adminService.enableLogin(id, request.getReason());
            } else {
                adminService.disableLogin(id, request.getReason());
            }
            return ResponseEntity.ok(new AuthDto.MessageResponse(
                    "User status updated to " + (request.isActive() ? "active" : "inactive"), true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id, @RequestBody SuspendRequest request) {
        try {
            LocalDateTime suspendedUntil;
            if (request.getDays() == null || request.getDays() <= 0) {
                // Indefinite suspension
                suspendedUntil = LocalDateTime.now().plusYears(100);
            } else {
                suspendedUntil = LocalDateTime.now().plusDays(request.getDays());
            }
            
            adminService.suspendUser(id, suspendedUntil, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse(
                    "User suspended until " + suspendedUntil.toLocalDate(), true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestBody BanRequest request) {
        try {
            adminService.banUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User banned successfully", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<?> unsuspendUser(@PathVariable Long id, @RequestBody UnsuspendRequest request) {
        try {
            adminService.unsuspendUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User suspension removed successfully", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long id, @RequestBody UnbanRequest request) {
        try {
            adminService.unbanUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User unbanned successfully", true));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/soft-delete")
    public ResponseEntity<?> softDeleteUser(@PathVariable Long id, @RequestBody DeleteRequest request) {
        try {
            adminService.softDeleteUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User soft deleted successfully", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/users/{id}/restore")
    public ResponseEntity<?> restoreUser(@PathVariable Long id, @RequestBody RestoreRequest request) {
        try {
            adminService.restoreUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User restored successfully", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> permanentDeleteUser(@PathVariable Long id, @RequestBody DeleteRequest request) {
        try {
            adminService.permanentDeleteUser(id, request.getReason());
            return ResponseEntity.ok(new AuthDto.MessageResponse("User permanently deleted successfully", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new AuthDto.MessageResponse(e.getMessage(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Request DTOs
    public static class RoleUpdateRequest {
        private String role;
        private String reason;
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class StatusUpdateRequest {
        private boolean active;
        private String reason;
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SuspendRequest {
        private Integer days;
        private String reason;
        public Integer getDays() { return days; }
        public void setDays(Integer days) { this.days = days; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class BanRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class UnsuspendRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class UnbanRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class DeleteRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RestoreRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
