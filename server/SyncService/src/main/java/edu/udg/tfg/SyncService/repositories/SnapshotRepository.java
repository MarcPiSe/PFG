package edu.udg.tfg.SyncService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.udg.tfg.SyncService.entities.SnapshotEntity;

import java.util.List;
import java.util.UUID;

public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {
    SnapshotEntity findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
