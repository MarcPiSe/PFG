package edu.udg.tfg.Trash.controllers.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrashResponse {
    private List<UUID> files;

    public TrashResponse(List<UUID> files) {
        this.files = files;
    }

    public TrashResponse() {
        files = new ArrayList<>();
    }

    public List<UUID> getFiles() {
        return files;
    }

    public void setFiles(List<UUID> files) {
        this.files = files;
    }
}
