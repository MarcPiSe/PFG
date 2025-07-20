package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.UUID;

public class FolderId {
    private UUID id;
    private FolderId folderId;

    public FolderId() {}

    public FolderId(UUID id, FolderId folderId) {
        this.id = id;
        this.folderId = folderId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FolderId getFolderId() {
        return folderId;
    }

    public void setFolderId(FolderId folderId) {
        this.folderId = folderId;
    }
}
