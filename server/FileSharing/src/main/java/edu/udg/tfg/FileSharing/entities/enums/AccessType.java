package edu.udg.tfg.FileSharing.entities.enums;

public enum AccessType {
    NONE(0),
    READ(1),
    WRITE(2),
    ADMIN(3);

    private final int value;

    AccessType(int value) {
        this.value = value;
    }
} 