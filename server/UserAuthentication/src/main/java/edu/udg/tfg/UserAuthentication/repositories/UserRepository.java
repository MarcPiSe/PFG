package edu.udg.tfg.UserAuthentication.repositories;

import edu.udg.tfg.UserAuthentication.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    List<UserEntity> findByUsernameContainingIgnoreCase(String username);
}
