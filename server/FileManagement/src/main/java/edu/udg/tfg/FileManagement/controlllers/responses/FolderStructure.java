package edu.udg.tfg.FileManagement.controlllers.responses;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FolderStructure {
    private UUID id;
    private String name;
    private List<FolderStructure> subfolders;

    public FolderStructure() {
    }

    public FolderStructure(UUID id, String name, List<FolderStructure> children) {
        this.id = id;
        this.name = name;
        this.subfolders = children;
    }

    public FolderStructure(UUID id, String name) {
        this.id = id;
        this.name = name;
        this.subfolders = new ArrayList<>();
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

    public List<FolderStructure> getSubfolders() {
        return subfolders;
    }

    public void setSubfolders(List<FolderStructure> subfolders) {
        this.subfolders = subfolders;
    }
}
