package edu.udg.tfg.FileSharing.feignClients.responses;

import java.util.UUID;

public class AccessResponse {
    private int accessType;
    private UUID userId;
    private UUID fieldId;
    private String username;
    private String email;

    public AccessResponse() {
    }

    public AccessResponse(int accessType, UUID userId, UUID fieldId) {
        this.accessType = accessType;
        this.userId = userId;
        this.fieldId = fieldId;
    }

    public int getAccessType() {
        return accessType;
    }

    public void setAccessType(int accessType) {
        this.accessType = accessType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getFieldId() {
        return fieldId;
    }

    public void setFieldId(UUID fieldId) {
        this.fieldId = fieldId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
