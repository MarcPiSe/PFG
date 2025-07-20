package edu.udg.tfg.FileManagement.feignClients.fileAccess;

import java.util.UUID;

public class AccessResponse {
    private int accessType;
    private UUID userId;
    private UUID fieldId;

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
}