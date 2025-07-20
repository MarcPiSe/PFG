package edu.udg.tfg.UserManagement.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Entity
public class UserDeletionProcess implements Serializable {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID userId;

    private String fileManagementStatus;
    private String fileSharingStatus;
    private String fileAccessControlStatus;
    private String userManagementStatus;
    private String userAuthenticationStatus;
    private String trashStatus;
    private String syncServiceStatus;
    private Date createdAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFileManagementStatus() {
        return fileManagementStatus;
    }

    public void setFileManagementStatus(String fileManagementStatus) {
        this.fileManagementStatus = fileManagementStatus;
    }

    public String getFileSharingStatus() {
        return fileSharingStatus;
    }

    public void setFileSharingStatus(String fileSharingStatus) {
        this.fileSharingStatus = fileSharingStatus;
    }

    public String getFileAccessControlStatus() {
        return fileAccessControlStatus;
    }

    public void setFileAccessControlStatus(String fileAccessControlStatus) {
        this.fileAccessControlStatus = fileAccessControlStatus;
    }

    public String getUserManagementStatus() {
        return userManagementStatus;
    }

    public void setUserManagementStatus(String userManagementStatus) {
        this.userManagementStatus = userManagementStatus;
    }

    public String getUserAuthenticationStatus() {
        return userAuthenticationStatus;
    }

    public void setUserAuthenticationStatus(String userAuthenticationStatus) {
        this.userAuthenticationStatus = userAuthenticationStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getTrashStatus() {
        return trashStatus;
    }

    public void setTrashStatus(String trashStatus) {
        this.trashStatus = trashStatus;
    }

    public String getSyncServiceStatus() {
        return syncServiceStatus;
    }

    public void setSyncServiceStatus(String syncServiceStatus) {
        this.syncServiceStatus = syncServiceStatus;
    }
} 