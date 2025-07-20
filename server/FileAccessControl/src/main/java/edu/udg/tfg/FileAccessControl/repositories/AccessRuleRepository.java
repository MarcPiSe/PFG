package edu.udg.tfg.FileAccessControl.repositories;// AccessRuleRepository.java

import edu.udg.tfg.FileAccessControl.entities.AccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessRuleRepository extends JpaRepository<AccessRule, UUID> {

    List<AccessRule> findByUserId(UUID userId);
    
    List<AccessRule> findByElementId(UUID fileId);

    Optional<AccessRule> findByElementIdAndUserId(UUID fileId, UUID userId);

    void deleteByElementIdAndUserId(UUID fileId, UUID userId);

    void deleteByUserId(UUID userId);
    
    void deleteByElementId(UUID elementId);
}
