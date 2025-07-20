package edu.udg.tfg.SyncService.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class SnapshotElementEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column
    private UUID elementId;
    
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    private SnapshotEntity snapshot;
    
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private SnapshotElementEntity parent;
    
    @Column
    private String type;
    @Column
    private String name;
    @Column
    private String hash;
    @Column
    private String path;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<SnapshotElementEntity> content;

    public SnapshotElementEntity() {
        this.content = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<SnapshotElementEntity> getContent() {
        return content;
    }

    public void setContent(List<SnapshotElementEntity> content) {
        this.content = content;
    }

    public SnapshotElementEntity getParent() {
        return parent;
    }

    public void setParent(SnapshotElementEntity parent) {
        this.parent = parent;
    }

    public SnapshotEntity getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SnapshotEntity snapshot) {
        this.snapshot = snapshot;
    }
}
