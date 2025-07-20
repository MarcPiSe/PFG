package edu.udg.tfg.Trash.controllers.responses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FolderInfo extends DetailsInfo{
    private List<FolderInfo> subfolders;
    private List<FileInfo> files;

    public FolderInfo(UUID id, String name, String type, Boolean isDirectory, UUID parent, Date createdAt, Date updatedAt, String mimeType, String accessLevel, Boolean shared, Date deletedAt, Date clientDeletedAt, String ownerUserName, String ownerUserEmail, UUID ownerUserId, Date expirationDate, List<FolderInfo> subfolders, List<FileInfo> files) {
        super(id, name, type, isDirectory, parent, createdAt, updatedAt, mimeType, accessLevel, shared, deletedAt, clientDeletedAt, ownerUserName, ownerUserEmail, ownerUserId, expirationDate);
        this.subfolders = subfolders;
        this.files = files;
    }

    public FolderInfo() {
        super();
    }

    public List<FolderInfo> getSubfolders() {
        return subfolders;
    }

    public void setSubfolders(List<FolderInfo> subfolders) {
        this.subfolders = subfolders;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }
}
