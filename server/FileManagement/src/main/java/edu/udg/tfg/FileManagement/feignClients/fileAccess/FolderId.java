package edu.udg.tfg.FileManagement.feignClients.fileAccess;

import java.util.UUID;

public class FolderId {
    private UUID id;
    private FolderId folderId;

    public FolderId() {
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
