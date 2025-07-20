package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.UUID;

import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;

public class SharedRequest {
    private UUID elementId;
    private UUID parentId;
    private String user; 
    private AccessType accessType;

    public SharedRequest() {}

    public SharedRequest(UUID elementId, UUID parentId, String shareWithUsername, AccessType accessType) {
        this.elementId = elementId;
        this.parentId = parentId;
        this.user = shareWithUsername;
        this.accessType = accessType;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String shareWithUsername) {
        this.user = shareWithUsername;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
