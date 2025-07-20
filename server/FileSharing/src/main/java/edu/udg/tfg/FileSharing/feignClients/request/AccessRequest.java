package edu.udg.tfg.FileSharing.feignClients.request;


import java.util.UUID;

public class AccessRequest {
    private UUID userId;
    private UUID elementId;
    private int accessType;

    public AccessRequest(UUID userId, UUID elementId, int accessType) {
        this.userId = userId;
        this.elementId = elementId;
        this.accessType = accessType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getElementId() {
        return elementId;
    }

    public int getAccessType() {    
        return accessType;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public void setAccessType(int accessType) {
        this.accessType = accessType;
    }
}
