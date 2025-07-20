package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.List;
import java.util.UUID;

public class SharedRequestSave {
    private UUID elementId;
    private FolderId folderId;
    private List<UUID> files;

    public SharedRequestSave() {}

    public SharedRequestSave(UUID elementId, FolderId folderId, List<UUID> files) {
        this.elementId = elementId;
        this.folderId = folderId;
        this.files = files;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public FolderId getFolderId() {
        return folderId;
    }

    public void setFolderId(FolderId folderId) {
        this.folderId = folderId;
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
