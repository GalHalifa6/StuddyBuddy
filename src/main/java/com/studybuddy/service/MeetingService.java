package com.studybuddy.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for generating meeting links for video conferencing platforms
 */
@Service
public class MeetingService {

    /**
     * Generate a stable Jitsi meeting room URL
     * Format: https://meet.jit.si/studybuddy-{sessionId}-{shortToken}
     * 
     * @param sessionId The session ID
     * @return Jitsi meeting room URL
     */
    public String generateJitsiMeetingLink(Long sessionId) {
        // Generate a short, stable token based on session ID
        // This ensures the same session always gets the same room URL
        String shortToken = generateShortToken(sessionId);
        String roomName = "studybuddy-" + sessionId + "-" + shortToken;
        return "https://meet.jit.si/" + roomName;
    }

    /**
     * Generate a short token from session ID for room name uniqueness
     */
    private String generateShortToken(Long sessionId) {
        // Use session ID to generate a deterministic short token
        // This ensures same session always gets same room
        UUID uuid = UUID.nameUUIDFromBytes(("studybuddy-session-" + sessionId).getBytes());
        return uuid.toString().substring(0, 8).replace("-", "");
    }
}

