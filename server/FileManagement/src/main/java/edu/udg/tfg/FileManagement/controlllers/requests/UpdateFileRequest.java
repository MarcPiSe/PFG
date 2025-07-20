package edu.udg.tfg.FileManagement.controlllers.requests;

import java.util.UUID;

public class UpdateFileRequest {
    private String name;
    private String parentId;

    public UpdateFileRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}