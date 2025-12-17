package com.studybuddy.controller;

import com.studybuddy.model.AllowedEmailDomain;
import com.studybuddy.repository.AllowedEmailDomainRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Controller for managing allowed email domains
 * Protected with ADMIN role
 */
@RestController
@RequestMapping("/api/admin/domains")
@PreAuthorize("hasRole('ADMIN')")
public class DomainAdminController {

    @Autowired
    private AllowedEmailDomainRepository domainRepository;

    /**
     * Get all allowed domains
     */
    @GetMapping
    public ResponseEntity<List<AllowedEmailDomain>> getAllDomains() {
        return ResponseEntity.ok(domainRepository.findAll());
    }

    /**
     * Get domain by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDomainById(@PathVariable Long id) {
        return domainRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Add a new domain
     */
    @PostMapping
    public ResponseEntity<?> addDomain(@Valid @RequestBody DomainRequest request) {
        // Check if domain already exists
        if (domainRepository.existsByDomain(request.getDomain())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Domain already exists"));
        }

        AllowedEmailDomain domain = new AllowedEmailDomain();
        domain.setDomain(request.getDomain().toLowerCase());
        domain.setStatus(request.getStatus() != null ? 
                request.getStatus() : AllowedEmailDomain.DomainStatus.ALLOW);
        domain.setInstitutionName(request.getInstitutionName());

        domainRepository.save(domain);
        return ResponseEntity.ok(domain);
    }

    /**
     * Update an existing domain
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDomain(@PathVariable Long id, 
                                          @Valid @RequestBody DomainRequest request) {
        return domainRepository.findById(id)
                .map(domain -> {
                    if (request.getDomain() != null) {
                        domain.setDomain(request.getDomain().toLowerCase());
                    }
                    if (request.getStatus() != null) {
                        domain.setStatus(request.getStatus());
                    }
                    if (request.getInstitutionName() != null) {
                        domain.setInstitutionName(request.getInstitutionName());
                    }
                    domainRepository.save(domain);
                    return ResponseEntity.ok(domain);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a domain
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id) {
        if (!domainRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        domainRepository.deleteById(id);
        return ResponseEntity.ok(new SuccessResponse("Domain deleted successfully"));
    }

    // DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainRequest {
        @NotBlank(message = "Domain is required")
        private String domain;
        
        private AllowedEmailDomain.DomainStatus status;
        
        private String institutionName;
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class SuccessResponse {
        private String message;
    }
}





