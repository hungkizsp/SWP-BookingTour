package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.UserResponse;

/**
 * Service to verify Google OAuth2 ID tokens and find-or-create users.
 */
public interface GoogleAuthService {
    /**
     * Verify a Google ID token and return the corresponding user.
     * If the user doesn't exist, create a new one from the Google profile.
     *
     * @param idToken the Google ID token from the frontend
     * @return the user response
     */
    UserResponse authenticateGoogleUser(String idToken);
}
