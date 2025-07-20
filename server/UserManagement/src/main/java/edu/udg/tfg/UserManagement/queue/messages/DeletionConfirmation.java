package edu.udg.tfg.UserManagement.queue.messages;

import java.io.Serializable;
import java.util.UUID;

public class DeletionConfirmation implements Serializable {
    private UUID userId;
    private String serviceName;

    public DeletionConfirmation() {}

    public DeletionConfirmation(UUID userId, String serviceName) {
        this.userId = userId;
        this.serviceName = serviceName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
} 