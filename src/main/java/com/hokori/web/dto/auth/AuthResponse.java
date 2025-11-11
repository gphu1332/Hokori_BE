package com.hokori.web.dto.auth;

import com.hokori.web.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private UserInfo user;
    private List<String> roles;

    @Data @NoArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String firebaseUid;
        private Boolean firebaseEmailVerified;
        private String firebaseProvider;
        private String headline;
        private String bio;
        private String currentJlptLevel;
        private Boolean isActive;

        public UserInfo(User u){
            this.id = u.getId();
            this.email = u.getEmail();
            this.username = u.getUsername();
            this.displayName = u.getDisplayName();
            this.avatarUrl = u.getAvatarUrl();
            this.firebaseUid = u.getFirebaseUid();
            this.firebaseEmailVerified = u.getFirebaseEmailVerified();
            this.firebaseProvider = u.getFirebaseProvider();
            this.headline = u.getHeadline();
            this.bio = u.getBio();
            this.currentJlptLevel = (u.getCurrentJlptLevel()!=null) ? u.getCurrentJlptLevel().name() : null;
            this.isActive = u.getIsActive();
        }
    }
}
