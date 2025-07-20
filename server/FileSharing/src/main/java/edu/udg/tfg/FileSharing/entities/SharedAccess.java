package edu.udg.tfg.FileSharing.entities;

import edu.udg.tfg.FileSharing.entities.enums.AccessType;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
public class SharedAccess {
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
    private UUID elementId;

    @Column
    private Date sharedDate;

    @Column
    private boolean root;

    public SharedAccess(UUID id, UUID userId, UUID elementId, Date sharedDate) {
        this.id = id;
        this.userId = userId;
        this.elementId = elementId;
        this.sharedDate = sharedDate;
    }

    public SharedAccess() {

    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getElementId() {
        return elementId;
    }

    public void setElementId(UUID elementId) {
        this.elementId = elementId;
    }

    public Date getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(Date sharedDate) {
        this.sharedDate = sharedDate;
    }
}
