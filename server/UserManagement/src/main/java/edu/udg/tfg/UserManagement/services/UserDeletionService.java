package edu.udg.tfg.UserManagement.services;

import edu.udg.tfg.UserManagement.queue.Sender;
import edu.udg.tfg.UserManagement.queue.messages.DeletionConfirmation;
import edu.udg.tfg.UserManagement.entities.UserDeletionProcess;
import edu.udg.tfg.UserManagement.repositories.UserDeletionProcessRepository;
import edu.udg.tfg.UserManagement.feignClients.userAuth.UserAuthClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserDeletionService {

    @Autowired
    private UserDeletionProcessRepository userDeletionProcessRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private Sender sender;

    @Autowired
    private UserAuthClient userAuthClient;

    public void startUserDeletion(UUID userId) {
        if (userDeletionProcessRepository.existsById(userId)) {
            return;
        }

        userAuthClient.deleteAccountInternal(userId);
        userService.deleteUserLocal(userId);

        UserDeletionProcess process = new UserDeletionProcess();
        process.setUserId(userId);
        process.setFileManagementStatus("PENDING");
        process.setFileSharingStatus("PENDING");
        process.setFileAccessControlStatus("PENDING");
        process.setUserManagementStatus("DONE");
        process.setUserAuthenticationStatus("DONE");
        process.setTrashStatus("PENDING");
        process.setSyncServiceStatus("PENDING");
        process.setCreatedAt(new Date());
        userDeletionProcessRepository.save(process);

        sender.sendDeleteCommandToFileManagement(userId);
        sender.sendDeleteCommandToFileSharing(userId);
        sender.sendDeleteCommandToFileAccessControl(userId);
        //sender.sendDeleteCommandToUserManagement(userId);
        //sender.sendDeleteCommandToUserAuthentication(userId);
        sender.sendDeleteCommandToTrash(userId);
        sender.sendDeleteCommandToSyncService(userId);
    }

    public void processDeletionConfirmation(DeletionConfirmation confirmation) {
        UserDeletionProcess process = userDeletionProcessRepository.findById(confirmation.getUserId())
                .orElse(null);

        if (process == null) return;

        if ("FileManagement".equals(confirmation.getServiceName())) {
            process.setFileManagementStatus("DONE");
        } else if ("FileSharing".equals(confirmation.getServiceName())) {
            process.setFileSharingStatus("DONE");
        } else if ("FileAccessControl".equals(confirmation.getServiceName())) {
            process.setFileAccessControlStatus("DONE");
        } else if ("UserManagement".equals(confirmation.getServiceName())) {
            process.setUserManagementStatus("DONE");
        } else if ("UserAuthentication".equals(confirmation.getServiceName())) {
            process.setUserAuthenticationStatus("DONE");
        } else if ("SyncService".equals(confirmation.getServiceName())) {
            process.setSyncServiceStatus("DONE");
        } else if ("Trash".equals(confirmation.getServiceName())) {
            process.setTrashStatus("DONE");
        }

        userDeletionProcessRepository.save(process);
    }

    public void retryPendingDeletions() {
        List<UserDeletionProcess> pendingProcesses = userDeletionProcessRepository.findByFileManagementStatusOrFileSharingStatusOrFileAccessControlStatusOrTrashStatusOrUserManagementStatusOrUserAuthenticationStatusOrSyncServiceStatus("PENDING", "PENDING", "PENDING", "PENDING", "PENDING", "PENDING", "PENDING");

        for (UserDeletionProcess process : pendingProcesses) {
            if ("PENDING".equals(process.getFileManagementStatus())) {
                sender.sendDeleteCommandToFileManagement(process.getUserId());
            }
            if ("PENDING".equals(process.getFileSharingStatus())) {
                sender.sendDeleteCommandToFileSharing(process.getUserId());
            }
            if ("PENDING".equals(process.getFileAccessControlStatus())) {
                sender.sendDeleteCommandToFileAccessControl(process.getUserId());
            }
            if ("PENDING".equals(process.getUserManagementStatus())) {
                sender.sendDeleteCommandToUserManagement(process.getUserId());
            }
            if ("PENDING".equals(process.getUserAuthenticationStatus())) {
                sender.sendDeleteCommandToUserAuthentication(process.getUserId());
            }
            if ("PENDING".equals(process.getSyncServiceStatus())) {
                sender.sendDeleteCommandToSyncService(process.getUserId());
            }
            if ("PENDING".equals(process.getTrashStatus())) {
                sender.sendDeleteCommandToTrash(process.getUserId());
            }
        }
    }

    public void cleanupCompletedDeletions() {
        List<UserDeletionProcess> completedProcesses = userDeletionProcessRepository.findByFileManagementStatusAndFileSharingStatusAndFileAccessControlStatusAndTrashStatusAndUserManagementStatusAndUserAuthenticationStatusAndSyncServiceStatus("DONE", "DONE", "DONE", "DONE", "DONE", "DONE", "DONE");
        
        userDeletionProcessRepository.deleteAll(completedProcesses);
    }
} 