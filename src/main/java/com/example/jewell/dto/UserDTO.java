package com.example.jewell.dto;

public class UserDTO {

    private Long id;
    private String profileImageUrl;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public UserDTO(Long id, String profileImageUrl) {
        this.id = id;
        this.profileImageUrl = profileImageUrl;
    }

}
