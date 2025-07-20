package edu.udg.tfg.FileManagement.controlllers.responses;

import java.util.List;
import java.util.UUID;

public class SetDeletedResponse {
    private List<UUID> elementIds;
    
    public SetDeletedResponse() {
    }
    
    public SetDeletedResponse(List<UUID> elementIds) {
        this.elementIds = elementIds;
    }
    
    public List<UUID> getElementIds() {
        return elementIds;
    }
    
    public void setElementIds(List<UUID> elementIds) {
        this.elementIds = elementIds;
    }
} 