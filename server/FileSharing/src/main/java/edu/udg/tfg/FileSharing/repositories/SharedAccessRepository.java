package edu.udg.tfg.FileSharing.repositories;

import edu.udg.tfg.FileSharing.entities.SharedAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedAccessRepository extends JpaRepository<SharedAccess, UUID> {
    List<SharedAccess> findByUserId(UUID userId);

    void deleteByElementIdAndUserId(UUID fileId, UUID userId);

    List<SharedAccess> findByUserIdAndRoot(UUID userId, boolean root);

    Optional<SharedAccess> findByElementIdAndUserId(UUID folderId, UUID userId);

    List<SharedAccess> findByElementId(UUID elementId);

    void deleteByUserId(UUID userId);
}
