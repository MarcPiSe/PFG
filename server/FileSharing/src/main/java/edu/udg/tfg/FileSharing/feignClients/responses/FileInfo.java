package edu.udg.tfg.FileSharing.feignClients.responses;

import jakarta.persistence.Column;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FileInfo extends DetailsInfo{
    private Long size;

    public FileInfo() {
        super();
    }

    public FileInfo(UUID id, String name, String type, Boolean isDirectory, UUID parent, Date createdAt, Date updatedAt, Long size, String mimeType, String accessLevel, Boolean shared, Date deletedAt, Date clientDeletedAt, String ownerUserName, String ownerUserEmail, UUID ownerUserId, Date expirationDate) {
        super(id, name, type, isDirectory, parent, createdAt, updatedAt, mimeType, accessLevel, shared, deletedAt, clientDeletedAt, ownerUserName, ownerUserEmail, ownerUserId, expirationDate);
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }


}
