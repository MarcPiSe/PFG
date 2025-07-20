package edu.udg.tfg.Trash.repositories;

import edu.udg.tfg.Trash.entities.RecordStatus;
import edu.udg.tfg.Trash.entities.TrashRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrashRecordRepository extends JpaRepository<TrashRecord, UUID> {
    List<TrashRecord> findByUserId(UUID userId);
    Optional<TrashRecord> findByElementIdAndUserId(UUID elementId, UUID userId);
    Optional<TrashRecord> findByUserIdAndElementId(UUID userId, UUID elementId);
    Optional<TrashRecord> findByElementId(UUID elementId);

    List<TrashRecord> findByExpirationDateLessThanEqual(Date expirationDate);

    List<TrashRecord> findByExpirationDateLessThanEqualAndStatus(Date date, RecordStatus status);

    List<TrashRecord> findByStatus(RecordStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM TrashRecord t WHERE t.status = 'PENDING_DELETION' AND t.access = true AND t.sharing = true AND t.manager = true")
    void clearPending();

    void deleteByElementIdAndUserId(UUID fileId, UUID userId);
    
    void deleteByUserIdAndElementIdIn(UUID userId, List<UUID> elementIds);

    List<TrashRecord> findByUserIdAndRoot(UUID userId, boolean root);

    List<TrashRecord> findByUserIdAndStatus(UUID userId, RecordStatus status);

    List<TrashRecord> findByUserIdAndElementIdIn(UUID userId, List<UUID> elementIds);

    void deleteByUserId(UUID userId);

    List<TrashRecord> findByDeletionDateBeforeAndAccessIsAndSharingIsAndManagerIs(Date date, boolean access, boolean sharing, boolean manager);
}
