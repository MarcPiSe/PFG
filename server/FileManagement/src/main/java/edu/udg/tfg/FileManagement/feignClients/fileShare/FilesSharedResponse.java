package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.List;
import java.util.UUID;

public class FilesSharedResponse {
    private List<UUID> files;

    public FilesSharedResponse(List<UUID> files) {
        this.files = files;
    }

    public FilesSharedResponse() {
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
