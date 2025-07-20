package edu.udg.tfg.FileManagement.services;

import edu.udg.tfg.FileManagement.controlllers.responses.DetailsInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderInfo;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserDTO;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserManagementClient;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessResponse;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.FileAccessClient;
import edu.udg.tfg.FileManagement.feignClients.trash.TrashClient;
import edu.udg.tfg.FileManagement.feignClients.trash.TrashDetailsResponse;
import edu.udg.tfg.FileManagement.feignClients.userAuth.UserAuthenticationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.udg.tfg.FileManagement.entities.mappers.FolderMapper;
import edu.udg.tfg.FileManagement.entities.mappers.FileMapper;

@Service
public class ElementMappingService {
    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;
    private final UserManagementClient userManagementClient;
    private final FileAccessClient fileAccessClient;
    private final TrashClient trashClient;

    public ElementMappingService(
            FolderMapper folderMapper,
            FileMapper fileMapper, UserManagementClient userManagementClient, FileAccessClient fileAccessClient, TrashClient trashClient
    ) {
        this.folderMapper = folderMapper;
        this.fileMapper = fileMapper;
        this.userManagementClient = userManagementClient;
        this.fileAccessClient = fileAccessClient;
        this.trashClient = trashClient;
    }

    public FolderInfo mapFolderWithDetails(FolderEntity folder, UUID callerId) {
        FolderInfo info = folderMapper.map(folder);

        setExternalInfo(info, folder.getElementId(), folder.getUserId(), callerId, folder.getShared(), folder.getDeleted());
        
            info.setSubfolders(folder.getChildren().stream()
            .map(child -> mapFolderWithDetails(child, callerId))
            .collect(Collectors.toList()));
            
        info.setFiles(folder.getFiles().stream()
            .map(file -> mapFileWithDetails(file, callerId))
            .collect(Collectors.toList()));
            
        return info;
    }

    private void setExternalInfo(DetailsInfo info, UUID elementId, UUID userId, UUID callerId, boolean isShared, boolean isDeleted) {
        AccessResponse accessResponse = fileAccessClient.getFileAccess(elementId.toString(), callerId.toString(), true);
        AccessType accessType = AccessType.values()[accessResponse.getAccessType()];
        if(!AccessType.NONE.equals(accessType)) info.setAccessLevel(accessType.toString());

        info.setShared(isShared);

        UserDTO owner = userManagementClient.get(userId);
        info.setOwnerUserName(owner.getUsername());
        info.setOwnerUserEmail(owner.getEmail());

                if (isDeleted) {
            TrashDetailsResponse trashDetailsResponse = trashClient.getTrashFile(callerId, elementId);
            info.setDeletedAt(trashDetailsResponse.getDeletedDate());
            info.setClientDeletedAt(trashDetailsResponse.getExpirationDate());
        }
    }

    public FileInfo mapFileWithDetails(FileEntity file, UUID userId) {
        FileInfo info = fileMapper.map(file);

        setExternalInfo(info, file.getElementId(), file.getUserId(), userId, file.getShared(), file.getDeleted());

        info.setSize(file.getSize());
        info.setMimeType(file.getMimeType());

        return info;
    }
}
