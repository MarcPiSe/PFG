package edu.udg.tfg.UserManagement.feignClients.userAuth.responses;

import java.util.UUID;

public class UserAuthInfoResponse {
    private UUID id;
    private String username;

    public UserAuthInfoResponse() {
    }

    public UserAuthInfoResponse(UUID id, String username) {
        this.id = id;
        this.username = username;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
} 