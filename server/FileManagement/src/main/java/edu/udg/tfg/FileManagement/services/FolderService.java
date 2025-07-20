package edu.udg.tfg.FileManagement.services;

import com.ctc.wstx.util.ElementId;
import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderStructure;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderInfo;
import edu.udg.tfg.FileManagement.feignClients.fileShare.SharedInfo;
import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.entities.mappers.FolderMapper;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.feignClients.fileShare.FileShareClient;
import edu.udg.tfg.FileManagement.feignClients.fileShare.SharedRequest;
import edu.udg.tfg.FileManagement.feignClients.trash.TrashClient;
import edu.udg.tfg.FileManagement.feignClients.trash.TrashDetailsResponse;
import edu.udg.tfg.FileManagement.queue.Sender;
import edu.udg.tfg.FileManagement.repositories.ElementRepository;
import edu.udg.tfg.FileManagement.repositories.FileRepository;
import edu.udg.tfg.FileManagement.repositories.FolderRepository;
import edu.udg.tfg.FileManagement.utils.FileUtil;
import edu.udg.tfg.FileManagement.utils.ZipUtil;
import jakarta.ws.rs.ForbiddenException;
import edu.udg.tfg.FileManagement.feignClients.trash.TrashRequest;
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import edu.udg.tfg.FileManagement.exceptions.CircularDependencyException;
import edu.udg.tfg.FileManagement.exceptions.AccessRuleCreationException;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserManagementClient;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserDTO;

@Service
public class FolderService {

    @Value("${file.zip.path}")
    private String zipPath;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private TrashClient trashClient;

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private FileAccessService fileAccessService;
    @Autowired
    private FileShareClient fileShareClient;
    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private Sender sender;

    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private CommandService commandService;
    @Autowired
    private FileService fileService;

    @Transactional
    public FolderEntity createFolder(String name, FolderEntity parent, UUID userId, boolean isRoot, String connectionId) {
        FolderEntity folderEntity = new FolderEntity();
        if(isRoot) {
            folderEntity.setName(name);
        } else {
            folderEntity.setName(checkName(name, parent, 1, name));
        }
        folderEntity.setUserId(userId);
        folderEntity.setParent(parent);
        ElementEntity id = new ElementEntity();
        id.setFolder(true);
        id = elementRepository.save(id);
        folderEntity.setElementId(id);
        
        FolderEntity folder = folderRepository.save(folderEntity);
        
        try {
            if(isRoot || userId.equals(parent.getUserId())) {
                fileAccessService.addFileAccess(folder.getElementId(), userId, AccessType.ADMIN);
            } else {
                fileShareClient.shareFileInternal(userId, connectionId, new SharedRequest(folder.getElementId(), parent.getElementId(), parent.getUserId().toString(), AccessType.WRITE));
                
                fileAccessService.addFileAccess(folder.getElementId(), userId, AccessType.WRITE);
                fileAccessService.addFileAccess(folder.getElementId(), parent.getUserId(), AccessType.ADMIN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to assign permissions to the new folder", e);
        }
        
        return folder;
    }

    private String checkName(String name, FolderEntity parent, int index, String original) {
        checkSpecialCharacters(name);
        List<FolderEntity> folders = folderRepository.findByParentAndNameAndDeleted(parent, name, false);
        if(folders.isEmpty()) return name;
        else return checkName(original + " (" + index + ")", parent, index + 1, original);
    }

    private void checkSpecialCharacters(String name) {
        char[] forbiddenChars = {'<', '>', ':', '"', '/', '\\', '|', '?', '*', '\0'};
        
        for (char c : forbiddenChars) {
            if (name.indexOf(c) != -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Name cannot contain the character: " + c);
            }
        }
        
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", 
                        "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", 
                        "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};
        
        String nameWithoutExt = name.split("\\.")[0].toUpperCase();
        for (String reserved : reservedNames) {
            if (nameWithoutExt.equals(reserved)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Name cannot be a system reserved name: " + reserved);
            }
        }
    }

    public FolderEntity updateFolderMetadata(UUID folderId, String name, UUID parentId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        
        if(parentId != null && !parentId.equals(folder.getParent().getElementId())) {
            FolderEntity newParent = folderRepository.findByElementId(new ElementEntity(parentId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            validateNoCircularDependency(folder, newParent);
            folder.setParent(newParent);
        }

        if(name != null && !name.equals("") && !name.equals(folder.getName())) {
            checkSpecialCharacters(name);
            if (folder.getParent() != null && folderRepository.findByParentAndNameAndDeleted(folder.getParent(), name, false).size() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder with the name '" + name + "' already exists in this folder.");
            }
            folder.setName(name);
        }
        folder.setLastModification(new Date());
        return folderRepository.save(folder);
    }

    public Resource downloadFolder(UUID folderId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        return ZipUtil.createZipFromElements(List.of(folder), fileUtil);
    }

    @Transactional
    public List<UUID> removeFolder(UUID folderId) throws IOException {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        List<UUID> ids = new ArrayList<>();
        removeFolderRecursively(folder, ids);
        return ids;
    }

    private void removeFolderRecursively(FolderEntity folder, List<UUID> ids) throws IOException {
                for (FolderEntity child : folder.getChildren()) {
            removeFolderRecursively(child, ids);
        }
        ids.addAll(fileService.deleteAll(folder.getFiles()));
        ids.add(folder.getElementId());
        folderRepository.delete(folder);
    }

    @Transactional
    public List<UUID> setDeletedState(UUID folderId, boolean deleted) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        if(!deleted) {
            Boolean exists = folderRepository.findByParentAndNameAndDeleted(folder.getParent(), folder.getName(), false).size() > 0;
            if(exists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A folder with the same name already exists in this folder.");
            }
        }
        List<UUID> ids = new ArrayList<>();
        setDeletedStateRecursively(folder, deleted, ids);
        return ids;
    }

    private void setDeletedStateRecursively(FolderEntity folder, boolean deleted, List<UUID> ids) {
        folder.setDeleted(deleted);
        folderRepository.save(folder);
        ids.add(folder.getElementId());

        for (FolderEntity childFolder : folder.getChildren()) {
            setDeletedStateRecursively(childFolder, deleted, ids);
        }

        for (FileEntity childFile : folder.getFiles()) {
            childFile.setDeleted(deleted);
            fileRepository.save(childFile);
            ids.add(childFile.getElementId());
        }
    }

    @Transactional
    public void setRemoveFolder(UUID folderId, UUID userId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        setRemoveFolderRecursively(folder);
    }

    private void setRemoveFolderRecursively(FolderEntity folder) {
        folder.setDeleted(true);
        for (FolderEntity child : folder.getChildren()) {
            setRemoveFolderRecursively(child);
        }
        folder.getFiles().forEach(f -> {
            f.setDeleted(true);
            fileRepository.save(f);
        });
        folderRepository.save(folder);
    }

    public void moveFile(UUID entityId, UUID folderId, UUID userId) {
        FolderEntity fileEntity = folderRepository.findByElementId(new ElementEntity(entityId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        
        validateNoCircularDependency(fileEntity, folder);

        fileEntity.setName(checkName(fileEntity.getName(), folder, 1, fileEntity.getName()));

        FolderEntity previousParent = fileEntity.getParent();
        fileEntity.setParent(folder);
        fileEntity.setLastModification(new Date());
        fileEntity = folderRepository.save(fileEntity);
    }

    private void validateNoCircularDependency(FolderEntity elementToMove, FolderEntity targetParent) {
        if (elementToMove.getId().equals(targetParent.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a folder to itself");
        }
        
        List<UUID> allDescendantIds = getAllDescendantFolderIds(elementToMove);

        if (allDescendantIds.contains(targetParent.getElementId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a folder to one of its own subfolders");
        }
    }
    
    private List<UUID> getAllDescendantFolderIds(FolderEntity folder) {
        List<UUID> descendantIds = new ArrayList<>();
        getAllDescendantFolderIdsRecursive(folder, descendantIds);
        return descendantIds;
    }
    
    private void getAllDescendantFolderIdsRecursive(FolderEntity folder, List<UUID> accumulator) {
        for (FolderEntity child : folder.getChildren()) {
            accumulator.add(child.getElementId());
            getAllDescendantFolderIdsRecursive(child, accumulator);
        }
    }

    public List<FileEntity> getFilesByFolder(UUID folderId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        return folder.getFiles();
    }

    public FolderEntity getFolderById(UUID folderId) {
        return folderRepository.findByElementId(new ElementEntity(folderId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
    }

    private void filterDeleted(FolderEntity folder, boolean deleted) {
        folder.getChildren().removeIf(f -> f.getDeleted() != deleted);
        folder.getChildren().forEach(f -> filterDeleted(f, deleted));
        folder.getFiles().removeIf(f -> f.getDeleted() != deleted);
    }

    public FolderEntity getRootFolder(UUID userId, boolean deleted) {
        FolderEntity folder = folderRepository.findByUserIdAndDeletedAndParentIsNull(userId, deleted).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        filterDeleted(folder, deleted);
        return folder;
    }

//    public FolderEntity getFolderByElementId(UUID elementId, boolean deleted) {
//        FolderEntity folder =  folderRepository.findByElementIdAndDeleted(new ElementEntity(elementId), deleted).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
//        filterDeleted(folder, deleted);
//        return folder;
//    }

    public FolderEntity getFolderByElementId(UUID elementId, boolean deleted) {
        ElementEntity element = elementRepository.findById(elementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found with id: " + elementId));

        if (element.isFolder()) {
            FolderEntity folder = folderRepository.findByElementIdAndDeleted(element, deleted)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
            filterDeleted(folder, deleted);
            return folder;
        } else {
            UUID parentFolderId = fileRepository.findParentFolderIdByFileElementId(elementId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found for file"));

            FolderEntity folder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found with id: " + parentFolderId));

            if (folder.getDeleted() != null && folder.getDeleted() != deleted) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder doesn't match deleted state");
            }

            filterDeleted(folder, deleted);
            return folder;
        }
    }

    public void setDetails(FolderInfo info, UUID folderId, UUID userId, boolean deleted, boolean shared) {
        List<FileInfo> files = new ArrayList<>();
        List<FolderInfo> folders = new ArrayList<>();
        for (FileInfo file : info.getFiles()) {
            AccessType accessType = AccessType.values()[fileAccessService.getFileAccess(file.getId(), userId, false)];
            if(accessType != AccessType.NONE) {
                file.setAccessLevel(accessType.name());
                if(deleted) {
                    TrashDetailsResponse trashFile = trashClient.getTrashFile(userId, file.getId());
                    if(trashFile == null) {
                        continue;
                    }
                    file.setDeletedAt(trashFile.getDeletedDate());
                    file.setExpirationDate(trashFile.getExpirationDate());
                } else if(shared) {
                    SharedInfo sharedInfo = fileShareClient.getSharedWithUser(userId.toString(), file.getId().toString());
                    if(sharedInfo == null) {
                        continue;
                    }
                    file.setOwnerUserName(sharedInfo.getUsername());
                    file.setOwnerUserEmail(sharedInfo.getEmail());
                    file.setSharedAt(sharedInfo.getSharedAt());
                    file.setAccessLevel(sharedInfo.getAccessType().name());
                } 
                files.add(file);
            } 
        }
        for (FolderInfo folder : info.getSubfolders()) {
            AccessType accessType = AccessType.values()[fileAccessService.getFileAccess(folder.getId(), userId, false)];
            if(accessType != AccessType.NONE) {
                folder.setAccessLevel(accessType.name());
                if(deleted) {
                    TrashDetailsResponse trashFile = trashClient.getTrashFile(userId, folder.getId());
                    if(trashFile == null) {
                        continue;
                    }
                    folder.setDeletedAt(trashFile.getDeletedDate());
                    folder.setExpirationDate(trashFile.getExpirationDate());
                } else if(shared) {
                    SharedInfo sharedInfo = fileShareClient.getSharedWithUser(userId.toString(), folder.getId().toString());
                    if(sharedInfo == null) {
                        continue;
                    }
                    folder.setOwnerUserName(sharedInfo.getUsername());
                    folder.setOwnerUserEmail(sharedInfo.getEmail());
                    folder.setSharedAt(sharedInfo.getSharedAt());
                    folder.setAccessLevel(sharedInfo.getAccessType().name());
                } 
                folders.add(folder);
            }
        }
        info.setFiles(files);
        info.setSubfolders(folders);
        info.setAccessLevel(AccessType.values()[fileAccessService.getFileAccess(folderId, userId, false)].name());
    }

    public FolderStructure getFolderStructure(UUID userId) {
        FolderEntity folder = getRootFolder(userId, false);
        return setStructure(folder);
    }

    private FolderStructure setStructure(FolderEntity folder) {
        FolderStructure folderStructure = new FolderStructure(folder.getElementId(), folder.getName());
        folder.getChildren().stream().filter(f -> !f.getDeleted()).forEach(folder1 -> folderStructure.getSubfolders().add(setStructure(folder1)));
        return folderStructure;
    }

    public void restore(UUID elementId, UUID userId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(elementId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        fileAccessService.checkAccessElement(folder.getUserId(), folder.getElementId(), true, AccessType.ADMIN);
        if (folder.getParent() != null && folder.getParent().getDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot restore a folder whose parent is in the trash. Please restore the parent folder first.");
        }
        List<UUID> elementIds = new ArrayList<>();
        restoreFolderRecursively(folder, elementIds);
        elementIds.add(folder.getElementId());
        trashClient.restoreFiles(userId, elementIds);
    }

    private void restoreFolderRecursively(FolderEntity folder, List<UUID> elementIds) {
        folder.setDeleted(false);
        folder.getFiles().stream()
                .filter(FileEntity::getDeleted)
                .forEach(file -> {
                    fileAccessService.checkAccessElement(file.getUserId(), file.getElementId(), false, AccessType.ADMIN);
                    file.setDeleted(false);
                    fileRepository.save(file);
                    elementIds.add(file.getElementId());
                });
        folder.getChildren().stream()
                .filter(FolderEntity::getDeleted)
                .forEach(toRestore -> {
                    fileAccessService.checkAccessElement(toRestore.getUserId(), toRestore.getElementId(), true, AccessType.ADMIN);
                    restoreFolderRecursively(toRestore, elementIds);
                    elementIds.add(toRestore.getElementId());
                });
        folderRepository.save(folder);
    }

    public FolderEntity createRootFolder(UUID userId) {
        if(folderRepository.findByUserIdAndDeletedAndParentIsNull(userId, false).isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Root folder already exists for this user");
        }
        return createFolder("/", null, userId, true, "");
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        folderRepository.deleteByUserId(userId);
    }

    @Transactional
    public FolderEntity copyFolder(UUID folderId, UUID newParentFolderId, UUID userId, String connectionId) throws IOException {
        FolderEntity folderToCopy = getFolderById(folderId);
        System.out.println(folderToCopy.getParent().getId());
        System.out.println(folderToCopy.getParent().getElementId());
        System.out.println(newParentFolderId);
        if(folderToCopy.getDeleted()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The folder is deleted");
        FolderEntity newParentFolder = getFolderById(newParentFolderId);
        validateNoCircularDependency(folderToCopy, newParentFolder);
        return copyFolderRecursive(folderToCopy, newParentFolder, userId, connectionId);
    }

    private FolderEntity copyFolderRecursive(FolderEntity folderToCopy, FolderEntity newParent, UUID userId, String connectionId) throws IOException {
        FolderEntity newFolder = createFolder(folderToCopy.getName(), newParent, userId, false, connectionId);

        for (FileEntity fileToCopy : folderToCopy.getFiles()) {
            if(fileToCopy.getDeleted()) continue;
            fileService.copyFile(fileToCopy.getElementId(), newFolder.getElementId(), userId);
        }

        for (FolderEntity subFolderToCopy : folderToCopy.getChildren()) {
            if(subFolderToCopy.getDeleted()) continue;
            copyFolderRecursive(subFolderToCopy, newFolder, userId, connectionId);
        }

        return newFolder;
    }

    public List<UUID> getChildren(UUID folderId) {
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId)).orElse(null);
        if(folder == null) return new ArrayList<>();
        List<UUID> children = new ArrayList<>();
        getChildrenRecursive(folder, children);
        return children;
    }

    private void getChildrenRecursive(FolderEntity folder, List<UUID> children) {
        children.addAll(folder.getChildren().stream().map(FolderEntity::getElementId).collect(Collectors.toList()));
        children.addAll(folder.getFiles().stream().map(FileEntity::getElementId).collect(Collectors.toList()));
        folder.getChildren().forEach(child -> getChildrenRecursive(child, children));
    }
}
