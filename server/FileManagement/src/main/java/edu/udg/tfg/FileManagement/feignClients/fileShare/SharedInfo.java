package edu.udg.tfg.FileManagement.feignClients.fileShare;

import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import java.util.Date;

public class SharedInfo {
    private String username;
    private String email;
    private Date sharedAt;
    private AccessType accessType;

    public SharedInfo() {}

    public SharedInfo(String username, String email, Date sharedAt, AccessType accessType) {
        this.username = username;
        this.email = email;
        this.sharedAt = sharedAt;
        this.accessType = accessType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public Date getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Date sharedAt) {
        this.sharedAt = sharedAt;
    }
} 