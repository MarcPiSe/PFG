package edu.udg.tfg.FileSharing.controllers.requests;

import edu.udg.tfg.FileSharing.entities.enums.AccessType;

import java.util.List;
import java.util.UUID;

public class SharedRequest {
    private UUID elementId;
    private UUID userId;
    private UUID parentId;
    private List<UUID> files;
    private boolean root;
    private AccessType accessType;

    public SharedRequest() {}

    public SharedRequest(UUID elementId, UUID parentId, List<UUID> files, boolean root) {
        this.elementId = elementId;
        this.parentId = parentId;
        this.files = files;
        this.root = root;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
