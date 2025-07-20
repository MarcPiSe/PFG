package edu.udg.tfg.FileManagement.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
public class FolderEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    ) 
    @Column(updatable = false, nullable = false)
    private UUID id;
    @Column
    private UUID userId;
    @Column
    private String name;
    @Column
    private Date creationDate;
    @Column
    private Date lastModification;
    @Column
    private Boolean shared;
    @Column
    private Boolean deleted;

    public FolderEntity() {
        children = new ArrayList<>();
        files = new ArrayList<>();
        creationDate = new Date();
        lastModification = creationDate;
        shared = false;
        deleted = false;
    }

    @ManyToOne // Assuming the column name in the FolderEntity table
    private FolderEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL) // Cascade deletion to files
    private List<FolderEntity> children;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL) // Cascade deletion to files
    private List<FileEntity> files;

    @OneToOne(fetch = FetchType.EAGER, cascade=CascadeType.ALL, orphanRemoval = true)
    private ElementEntity elementId;

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public UUID getElementId() {
        return elementId.getId();
    }

    public void setElementId(ElementEntity elementId) {
        this.elementId = elementId;
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

    public FolderEntity getParent() {
        return parent;
    }

    public void setParent(FolderEntity parent) {
        this.parent = parent;
    }

    public List<FolderEntity> getChildren() {
        return children;
    }

    public void setChildren(List<FolderEntity> children) {
        this.children = children;
    }

    public List<FileEntity> getFiles() {
        return files;
    }

    public void setFiles(List<FileEntity> files) {
        this.files = files;
    }

    public Date getLastModification() {
        return lastModification;
    }

    public void setLastModification(Date lastModification) {
        this.lastModification = lastModification;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
