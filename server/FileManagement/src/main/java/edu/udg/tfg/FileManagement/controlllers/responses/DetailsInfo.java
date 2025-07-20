package edu.udg.tfg.FileManagement.controlllers.responses;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DetailsInfo {
    private UUID id;
    private String name;
    private String type;  
    private Boolean isDirectory;
    private UUID parent;
    private Date createdAt;
    private Date updatedAt;
    private String mimeType;
    private String accessLevel;
    private Boolean shared;
    private Date deletedAt;
    private Date clientDeletedAt;
    private String ownerUserName;
    private String ownerUserEmail;
    private Date expirationDate;
    private Date sharedAt;

    public DetailsInfo() {
    }

    public DetailsInfo(UUID id, String name, String type, Boolean isDirectory, UUID parent, Date createdAt, Date updatedAt, String mimeType, String accessLevel, Boolean shared, Date deletedAt, Date clientDeletedAt, String ownerUserName, String ownerUserEmail, Date sharedAt, Date expirationDate) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.isDirectory = isDirectory;
        this.parent = parent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.mimeType = mimeType;
        this.accessLevel = accessLevel;
        this.shared = shared;
        this.deletedAt = deletedAt;
        this.clientDeletedAt = clientDeletedAt;
        this.ownerUserName = ownerUserName;
        this.ownerUserEmail = ownerUserEmail;
        this.expirationDate = expirationDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public UUID getParent() {
        return parent;
    }

    public void setParent(UUID parent) {
        this.parent = parent;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Date getClientDeletedAt() {
        return clientDeletedAt;
    }

    public void setClientDeletedAt(Date clientDeletedAt) {
        this.clientDeletedAt = clientDeletedAt;
    }

    public String getOwnerUserName() {
        return ownerUserName;
    }

    public void setOwnerUserName(String ownerUserName) {
        this.ownerUserName = ownerUserName;
    }

    public String getOwnerUserEmail() {
        return ownerUserEmail;
    }

    public void setOwnerUserEmail(String ownerUserEmail) {
        this.ownerUserEmail = ownerUserEmail;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Date sharedAt) {
        this.sharedAt = sharedAt;
    }
}
