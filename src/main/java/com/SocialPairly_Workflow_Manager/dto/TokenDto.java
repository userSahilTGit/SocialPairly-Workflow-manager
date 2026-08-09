package com.SocialPairly_Workflow_Manager.dto;

public class TokenDto {
    private String idToken;
    private Boolean rememberMe;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public Boolean getRememberMe() { return rememberMe; }
    public void setRememberMe(Boolean rememberMe) { this.rememberMe = rememberMe; }
}