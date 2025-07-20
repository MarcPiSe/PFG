package edu.udg.tfg.FileManagement.repositories;

import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<FolderEntity, UUID> {

    FolderEntity findByName(String name);
    Optional<FolderEntity> findById(UUID id);

    Optional<FolderEntity> findByUserIdAndParentIsNull(UUID userId);

    Optional<FolderEntity> findByElementId(ElementEntity elementEntity);

    List<FolderEntity> findByParentAndNameAndDeleted(FolderEntity parent, String name, boolean deleted);

    Optional<FolderEntity> findByUserIdAndDeletedAndParentIsNull(UUID userId, boolean deleted);

    Optional<FolderEntity> findByElementIdAndDeleted(ElementEntity elementEntity, boolean deleted);

    void deleteByUserId(UUID userId);
}