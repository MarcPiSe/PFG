package edu.udg.tfg.FileSharing.controllers.requests;

import edu.udg.tfg.FileSharing.entities.enums.AccessType;

import java.util.UUID;

public class ShareRequest {
    private UUID elementId;
    private String user; 
    private UUID parentId;
    private AccessType accessType;

    public ShareRequest() {}

    public ShareRequest(UUID elementId, UUID parentId, String shareWithUsername, AccessType accessType) {
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