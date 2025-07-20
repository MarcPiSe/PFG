package edu.udg.tfg.FileSharing.controllers.requests;

import edu.udg.tfg.FileSharing.entities.enums.AccessType;

import java.util.UUID;

public class UpdateShareRequestDTO {
    private UUID elementId;
    private String usernameToUpdate; 
    private AccessType newAccessType;

    public UpdateShareRequestDTO() {}

    public UpdateShareRequestDTO(UUID elementId, String usernameToUpdate, AccessType newAccessType) {
        this.elementId = elementId;
        this.usernameToUpdate = usernameToUpdate;
        this.newAccessType = newAccessType;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public String getUsernameToUpdate() {
        return usernameToUpdate;
    }

    public void setUsernameToUpdate(String usernameToUpdate) {
        this.usernameToUpdate = usernameToUpdate;
    }

    public AccessType getNewAccessType() {
        return newAccessType;
    }

    public void setNewAccessType(AccessType newAccessType) {
        this.newAccessType = newAccessType;
    }
} 