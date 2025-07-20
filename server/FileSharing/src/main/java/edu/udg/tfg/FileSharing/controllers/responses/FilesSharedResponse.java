package edu.udg.tfg.FileSharing.controllers.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FilesSharedResponse {
    private List<UUID> files;

    public FilesSharedResponse() {
        files = new ArrayList<>();
    }

    public FilesSharedResponse(List<UUID> files) {
        this.files = files;
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
