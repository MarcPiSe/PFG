package edu.udg.tfg.SyncService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.udg.tfg.SyncService.entities.SnapshotEntity;
import edu.udg.tfg.SyncService.entities.SnapshotElementEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnapshotElementRepository extends JpaRepository<SnapshotElementEntity, UUID> {
    Optional<SnapshotElementEntity> findBySnapshotIdAndElementId(UUID snapshotId, UUID elementId);
    
    void deleteBySnapshotIdAndElementId(UUID snapshotId, UUID elementId);
    
    Optional<SnapshotElementEntity> findByElementId(UUID elementId);

    @Modifying
    @Query("DELETE FROM SnapshotElementEntity se WHERE se.snapshot.userId = :userId")
    void deleteBySnapshotUserId(@Param("userId") UUID userId);
    
    @Query("SELECT se FROM SnapshotElementEntity se WHERE se.snapshot.userId = :userId")
    List<SnapshotElementEntity> findBySnapshotUserId(@Param("userId") UUID userId);
}
