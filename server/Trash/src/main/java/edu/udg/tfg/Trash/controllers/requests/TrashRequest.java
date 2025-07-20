package edu.udg.tfg.Trash.controllers.requests;

import java.util.List;
import java.util.UUID;

public class TrashRequest {
    private UUID elementId;
    private List<UUID> ids;

    public TrashRequest() {}

    public TrashRequest(UUID elementId, List<UUID> ids) {
        this.elementId = elementId;
        this.ids = ids;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public List<UUID> getIds() {
        return ids;
    }

    public void setIds(List<UUID> files) {
        this.ids = files;
    }
}
