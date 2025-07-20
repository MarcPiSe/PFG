package edu.udg.tfg.FileManagement.feignClients.trash;

import java.util.List;
import java.util.UUID;

public class TrashResponse {
    private List<UUID> files;

    public TrashResponse() {
    }

    public TrashResponse(List<UUID> files) {
        this.files = files;
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
