package edu.udg.tfg.Trash.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Entity
public class TrashRecord {
    
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
    private boolean sharing;

    @Column
    private boolean access;

    @Column
    private boolean manager;

    @Column
    private boolean root;

    @Column
    private Date expirationDate;

    @Column
    private Date deletionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status = RecordStatus.ACTIVE;

    public TrashRecord(UUID userId, UUID elementId, boolean sharing, boolean access, boolean manager) {
        this.userId = userId;
        this.elementId = elementId;
        this.sharing = sharing;
        this.access = access;
        this.manager = manager;
        Calendar c= Calendar.getInstance();
        c.add(Calendar.DATE, 30);
        expirationDate = c.getTime();
        deletionDate = new Date();
        this.status = RecordStatus.ACTIVE;
    }

    public TrashRecord(UUID userId, UUID elementId, boolean isRoot) {
        this.userId = userId;
        this.elementId = elementId;
        this.root = isRoot;
        this.sharing = false;
        this.access = false;
        this.manager = false;
        Calendar c= Calendar.getInstance();
        c.add(Calendar.DATE, 30);
        expirationDate = c.getTime();
        deletionDate = new Date();
        this.status = RecordStatus.ACTIVE;
    }

    public TrashRecord() {}

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

    public boolean isSharing() {
        return sharing;
    }

    public void setSharing(boolean sharing) {
        this.sharing = sharing;
    }

    public boolean isAccess() {
        return access;
    }

    public void setAccess(boolean access) {
        this.access = access;
    }

    public boolean isManager() {
        return manager;
    }

    public void setManager(boolean manager) {
        this.manager = manager;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Date getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(Date deletionDate) {
        this.deletionDate = deletionDate;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }
}
