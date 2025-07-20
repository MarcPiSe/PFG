package edu.udg.tfg.UserManagement.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.udg.tfg.UserManagement.entities.UserDeletionProcess;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDeletionProcessRepository extends JpaRepository<UserDeletionProcess, UUID> {
    List<UserDeletionProcess> findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus);
    
    List<UserDeletionProcess> findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatusAndUserManagementStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus, String userManagementStatus);
    
    List<UserDeletionProcess> findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatusAndUserManagementStatusAndUserAuthenticationStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus, String userManagementStatus, String userAuthenticationStatus);

    List<UserDeletionProcess> findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatusAndUserManagementStatusAndUserAuthenticationStatusAndSyncServiceStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus, String userManagementStatus, String userAuthenticationStatus, String syncServiceStatus);

    List<UserDeletionProcess> findByFileManagementStatusOrFileSharingStatusOrFileAccessControlStatusOrTrashStatusOrUserManagementStatusOrUserAuthenticationStatusOrSyncServiceStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus, String trashStatus, String userManagementStatus, String userAuthenticationStatus, String syncServiceStatus);

    List<UserDeletionProcess> findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatusAndTrashStatusAndUserManagementStatusAndUserAuthenticationStatusAndSyncServiceStatus(
            String fileManagementStatus, String fileSharingStatus, String fileAccessControlStatus, String trashStatus, String userManagementStatus, String userAuthenticationStatus, String syncServiceStatus);
} 