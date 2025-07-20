package edu.udg.tfg.FileManagement.controlllers.requests;

import java.util.UUID;

public class ShareRequest {
    UUID elementId;
    boolean isFile;
    String shareUsername;
    int accessLevel;

    public ShareRequest(UUID elementId, boolean isFile, String shareUsername, int accessLevel) {
        this.elementId = elementId;
        this.isFile = isFile;
        this.shareUsername = shareUsername;
        this.accessLevel = accessLevel;
    }

    public ShareRequest() {
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getShareUsername() {
        return shareUsername;
    }

    public void setShareUsername(String shareUsername) {
        this.shareUsername = shareUsername;
    }
}
