package edu.udg.tfg.FileSharing.services;

import edu.udg.tfg.FileSharing.controllers.requests.ShareRequest;
import edu.udg.tfg.FileSharing.controllers.requests.UpdateShareRequestDTO;
import edu.udg.tfg.FileSharing.controllers.responses.SharedInfo;
import edu.udg.tfg.FileSharing.entities.SharedAccess;
import edu.udg.tfg.FileSharing.entities.enums.AccessType;
import edu.udg.tfg.FileSharing.feignClients.*;
import edu.udg.tfg.FileSharing.feignClients.request.AccessRequest;
import edu.udg.tfg.FileSharing.feignClients.responses.AccessResponse;
import edu.udg.tfg.FileSharing.feignClients.responses.DetailsInfo;
import edu.udg.tfg.FileSharing.feignClients.responses.FolderInfo;
import edu.udg.tfg.FileSharing.feignClients.responses.FileInfo;
import edu.udg.tfg.FileSharing.repositories.SharedAccessRepository;
import feign.FeignException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileSharingService {

    @Autowired
    private SharedAccessRepository sharedAccessRepository;

    @Autowired
    private FileAccessControlClient fileAccessControlClient;

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private FileManagementClient fileManagementClient;

    @Autowired
    private CommandService commandService;

    @Transactional
    public void shareElement(UUID userId, ShareRequest shareRequest, String connectionId) {
        AccessResponse requesterAccess = fileAccessControlClient.getFileAccess(
            shareRequest.getElementId(), userId);

        if (requesterAccess == null || requesterAccess.getAccessType() != AccessType.ADMIN.ordinal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required to share this element");
        }

        UUID shareWithUserId;
        try {
            shareWithUserId = userManagementClient.getUserId(shareRequest.getUser());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        AccessRequest accessRequest = new AccessRequest(shareWithUserId, shareRequest.getElementId(), shareRequest.getAccessType().ordinal());
        
        fileAccessControlClient.createAccess(accessRequest);

        SharedAccess sharedAccess = sharedAccessRepository.findByElementIdAndUserId(shareRequest.getElementId(), shareWithUserId).orElse(null);
        if(sharedAccess == null) {
            sharedAccess = new SharedAccess();
        }
        sharedAccess.setElementId(shareRequest.getElementId());
        sharedAccess.setUserId(shareWithUserId);
        FolderInfo parent;
        if (shareRequest.getParentId() == null) {
            parent = fileManagementClient.getParent(shareRequest.getElementId());
        } else {
            parent = fileManagementClient.getFolder(shareRequest.getParentId());
        }

        boolean isRootShared;
        if (parent != null) {
            Optional<SharedAccess> parentSharedAccess = sharedAccessRepository.findByElementIdAndUserId(parent.getId(), userId);
            isRootShared = !parentSharedAccess.isPresent();
        } else {
            // If no parent folder, this element is considered root shared
            isRootShared = true;
        }

        sharedAccess.setRoot(isRootShared);

        try {
            List<UUID> ids = fileManagementClient.getChildren(shareRequest.getElementId());
            updateChildren(ids, shareWithUserId, shareRequest.getAccessType().ordinal());
        } catch (FeignException.NotFound e) {}
        catch (Exception e) {
            throw e;
        }
        sharedAccessRepository.save(sharedAccess);
        String parentIdString = (parent != null) ? parent.getId().toString() : "root";

        commandService.sendShared(shareRequest.getElementId(), connectionId, userId.toString(), parentIdString);

    }

    @Transactional
    public void shareElementInternal(UUID userId, ShareRequest shareRequest, String connectionId) {
        FolderInfo parent;
        if(shareRequest.getParentId() == null) {
            parent = fileManagementClient.getParent(shareRequest.getElementId());
        } else {
            parent = fileManagementClient.getFolder(shareRequest.getParentId());
        }
        AccessResponse requesterAccess = fileAccessControlClient.getFileAccess(parent.getId(), userId);

        if (requesterAccess == null || requesterAccess.getAccessType() < 2) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required to share this element");
        }

        AccessRequest accessRequest = new AccessRequest(userId, shareRequest.getElementId(), shareRequest.getAccessType().ordinal());
        
        fileAccessControlClient.createAccess(accessRequest);

        SharedAccess sharedAccess = new SharedAccess();
        sharedAccess.setElementId(shareRequest.getElementId());
        sharedAccess.setUserId(userId);

        Optional<SharedAccess> parentSharedAccess = sharedAccessRepository.findByElementIdAndUserId(parent.getId(), userId);
        if(parentSharedAccess.isPresent()) {
            sharedAccess.setRoot(false);
        } else {
            sharedAccess.setRoot(true);
        }
        sharedAccess.setSharedDate(new Date());

                
        sharedAccessRepository.save(sharedAccess);
        commandService.sendShared(shareRequest.getElementId(), connectionId, userId.toString(), parent.getId().toString());
    }

    private void updateChildren(List<UUID> ids, UUID shareWithUserId, int accessType) {
        List<AccessRequest> accessRequests = new ArrayList<>();
        List<SharedAccess> sharedAccesses = new ArrayList<>();
        for(UUID id : ids) {
            AccessRequest accessRequest = new AccessRequest(shareWithUserId, id, accessType);
            accessRequests.add(accessRequest);
            SharedAccess folderSharedAccess = new SharedAccess();
            folderSharedAccess.setElementId(id);
            folderSharedAccess.setUserId(shareWithUserId);
            folderSharedAccess.setRoot(false);
            folderSharedAccess.setSharedDate(new Date());
            sharedAccesses.add(folderSharedAccess);
        }

        fileAccessControlClient.createAccessList(accessRequests);
        sharedAccessRepository.saveAll(sharedAccesses);
    }

    public FolderInfo getSharedWithUserRoot(UUID userId) {
        List<SharedAccess> sharedAccesses = sharedAccessRepository.findByUserIdAndRoot(userId, true);
        List<SharedAccess> sharedAccessesNotRoot = sharedAccessRepository.findByUserIdAndRoot(userId, false);
        List<UUID> elementIds = sharedAccesses.stream()
            .map(SharedAccess::getElementId)
            .collect(Collectors.toList());
        Map<UUID, SharedAccess> map = new HashMap<>();
        for (SharedAccess sharedAccess : sharedAccesses) {
            map.put(sharedAccess.getElementId(), sharedAccess);
        }
        for (SharedAccess sharedAccess : sharedAccessesNotRoot) {
            map.put(sharedAccess.getElementId(), sharedAccess);
        }

        if (elementIds.isEmpty()) {
            return new FolderInfo();
        }

        try {
            FolderInfo folderInfo = fileManagementClient.getFolderStructure(elementIds, false);
            setDetails(folderInfo, map);
            return folderInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return new FolderInfo();
        }
    }

    private void setDetails(FolderInfo folderInfo, Map<UUID, SharedAccess> map) {
        List<FileInfo> files = new ArrayList<>();
        List<FolderInfo> folders = new ArrayList<>();
        for (FileInfo file : folderInfo.getFiles()) {
            if(map.get(file.getId()) == null) {
                continue;
            }
            UserDetails userDetails = userManagementClient.getUser(map.get(file.getId()).getUserId());
            file.setShared(true);
            file.setOwnerUserName(userDetails.getUsername());
            file.setOwnerUserEmail(userDetails.getEmail());
            file.setAccessLevel(AccessType.values()[fileAccessControlClient.getFileAccess(file.getId(), map.get(file.getId()).getUserId()).getAccessType()].name());
            files.add(file);
        }
        for (FolderInfo folder : folderInfo.getSubfolders()) {
            if(map.get(folder.getId()) == null) {
                continue;
            }
            UserDetails userDetails = userManagementClient.getUser(map.get(folder.getId()).getUserId());
            folder.setShared(true);
            folder.setOwnerUserName(userDetails.getUsername());
            folder.setOwnerUserEmail(userDetails.getEmail());
            folder.setAccessLevel(AccessType.values()[fileAccessControlClient.getFileAccess(folder.getId(), map.get(folder.getId()).getUserId()).getAccessType()].name());
            setDetails(folder, map);
            folders.add(folder);
        }
        folderInfo.setFiles(files);
        folderInfo.setSubfolders(folders);
    }

    public List<SharedInfo> getUsersSharedWithElement(UUID elementId) {
        List<SharedAccess> sharedAccesses = sharedAccessRepository.findByElementId(elementId);
        List<SharedInfo> result = new ArrayList<>();

        for (SharedAccess sharedAccess : sharedAccesses) {
            try {
                UserDetails userDetails = userManagementClient.getUser(sharedAccess.getUserId());

                AccessResponse accessResponse = fileAccessControlClient.getFileAccess(
                    elementId, sharedAccess.getUserId());

                if (accessResponse != null) {
                    SharedInfo dto = new SharedInfo(
                        userDetails.getUsername(), 
                        userDetails.getEmail(),
                        sharedAccess.getSharedDate(),
                        AccessType.values()[accessResponse.getAccessType()]
                    );
                    result.add(dto);
                }
            } catch (Exception e) {
                continue;
            }
        }

        return result;
    }

    public SharedInfo getSharedWithUserAndElementId(UUID userId, UUID elementId) {
        Optional<SharedAccess> sharedAccess = sharedAccessRepository.findByElementIdAndUserId(elementId, userId);
        if(sharedAccess.isPresent()) {
            UserDetails userDetails = userManagementClient.getUser(sharedAccess.get().getUserId());
            AccessResponse accessResponse = fileAccessControlClient.getFileAccess(elementId, sharedAccess.get().getUserId());
            SharedInfo dto = new SharedInfo(userDetails.getUsername(), userDetails.getEmail(), sharedAccess.get().getSharedDate(), AccessType.values()[accessResponse.getAccessType()]);
            return dto;
        }
        return null;
    }

    public void updateShareAccess(UUID requesterId, UpdateShareRequestDTO updateRequest) {
        AccessResponse requesterAccess = fileAccessControlClient.getFileAccess(
            updateRequest.getElementId(), requesterId);

        if (requesterAccess == null || requesterAccess.getAccessType() != AccessType.ADMIN.ordinal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required to modify access rights");
        }

        UUID userIdToUpdate;
        try {
            userIdToUpdate = userManagementClient.getUserId(updateRequest.getUsernameToUpdate());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        SharedAccess sharedAccess = sharedAccessRepository.findByElementIdAndUserId(updateRequest.getElementId(), userIdToUpdate).orElse(null);
        if(sharedAccess != null) {
            sharedAccess.setSharedDate(new Date());
            sharedAccessRepository.save(sharedAccess);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not have access to this element");
        }

        AccessRequest accessRequest = new AccessRequest(userIdToUpdate, updateRequest.getElementId(), updateRequest.getNewAccessType().ordinal());
        
        fileAccessControlClient.modifyAccess(accessRequest);
    }

    @Transactional
    public void revokeShare(UUID requesterId, UUID elementId, String usernameToRemove, String connectionId) {
        UUID userIdToRemove;
        try {
            userIdToRemove = userManagementClient.getUserId(usernameToRemove);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (!requesterId.equals(userIdToRemove)) {
            AccessResponse requesterAccess = fileAccessControlClient.getFileAccess(elementId, requesterId);
            if (requesterAccess == null || requesterAccess.getAccessType() != 3) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin permission required to revoke access rights");
            }
        }
        
        fileAccessControlClient.deleteAccess(elementId, userIdToRemove);

        sharedAccessRepository.deleteByElementIdAndUserId(elementId, userIdToRemove);

        try {
            List<UUID> children = fileManagementClient.getChildren(elementId);
            revokeChildrenAccess(children, userIdToRemove);
        } catch (FeignException.NotFound e) {}
        catch (Exception e) {
            throw e;
        }

        commandService.sendUnshared(elementId, connectionId, requesterId.toString());
    }

    private void revokeChildrenAccess(List<UUID> children, UUID userId) {
        for(UUID child : children) {
            fileAccessControlClient.deleteAccess(child, userId);
            sharedAccessRepository.deleteByElementIdAndUserId(child, userId);
        }
    }

    public void handleElementDeleted(UUID elementId) {
        List<SharedAccess> sharedAccesses = sharedAccessRepository.findByElementId(elementId);
        sharedAccessRepository.deleteAll(sharedAccesses);
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        sharedAccessRepository.deleteByUserId(userId);
    }
}