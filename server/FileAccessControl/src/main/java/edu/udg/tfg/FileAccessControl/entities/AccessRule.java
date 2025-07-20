package edu.udg.tfg.FileAccessControl.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
public class AccessRule {
    
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

    @Enumerated(EnumType.STRING)
    @Column
    private AccessType accessType;

    public AccessRule(UUID id, UUID userId, UUID elementId, AccessType accessType) {
        this.id = id;
        this.userId = userId;
        this.elementId = elementId;
        this.accessType = accessType;
    }

    // Constructors
    public AccessRule() {
        this.id = null;
        this.userId = null;
        this.elementId = null;
        this.accessType = AccessType.NONE;
    }

    public static AccessRule none() {
        return new AccessRule(null, null, null, AccessType.NONE);
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

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
