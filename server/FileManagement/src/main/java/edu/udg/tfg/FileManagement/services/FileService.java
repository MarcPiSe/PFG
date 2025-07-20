package edu.udg.tfg.FileManagement.services;

import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.entities.mappers.FileMapper;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.feignClients.fileShare.FileShareClient;
import edu.udg.tfg.FileManagement.feignClients.fileShare.SharedRequest;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserManagementClient;
import edu.udg.tfg.FileManagement.feignClients.UserManagement.UserDTO;
import edu.udg.tfg.FileManagement.queue.Sender;
import edu.udg.tfg.FileManagement.repositories.ElementRepository;
import edu.udg.tfg.FileManagement.repositories.FileRepository;
import edu.udg.tfg.FileManagement.repositories.FolderRepository;
import edu.udg.tfg.FileManagement.utils.FileUtil;
import edu.udg.tfg.FileManagement.utils.ZipUtil;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import edu.udg.tfg.FileManagement.exceptions.AccessRuleCreationException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FolderRepository folderRepository;

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
    private FileMapper fileMapper;

    @Autowired
    private CommandService commandService;

    /**
     * Fase 6.2: Creación atómica de archivo con permisos
     * Si falla la creación de permisos, se hace rollback del archivo
     */
    @Transactional
    public FileEntity createFile(String name, String contentType, Long size, UUID folderId, UUID userId, String connectionId) {

        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        FileEntity file = new FileEntity();
        file.setName(checkName(name, folder, 1, name));
        file.setSize(size);
        file.setParent(folder);
        file.setUserId(folder.getUserId());
        file.setMimeType(contentType);
        ElementEntity id = new ElementEntity();
        id.setFolder(false);
        id = elementRepository.save(id);
        file.setElementId(id);
        file.setDeleted(false);
        file.setShared(false);

        FileEntity fileEntity = fileRepository.save(file);
        
        try {
            if(userId.equals(folder.getUserId())) {
                fileAccessService.addFileAccess(fileEntity.getElementId(), userId, AccessType.ADMIN);
            } else {
                UserDTO userDetails = userManagementClient.get(userId);
                fileShareClient.shareFileInternal(userId, connectionId, new SharedRequest(file.getElementId(), folder.getElementId(), userDetails.getUsername(), AccessType.WRITE));
                fileAccessService.addFileAccess(fileEntity.getElementId(), userId, AccessType.WRITE);
                fileAccessService.addFileAccess(fileEntity.getElementId(), folder.getUserId(), AccessType.ADMIN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not assign owner permission to new file");
        }
        
        return fileEntity;
    }

    public FileEntity createFileFromStream(String name, String contentType, Long size, UUID folderId, UUID userId, InputStream inputStream, String connectionId) throws IOException {
        FileEntity fileEntity = createFile(name, contentType, size, folderId, userId, connectionId);
        try {
            fileUtil.storeFile(fileEntity.getId(), inputStream);
        } catch (IOException e) {
            fileRepository.delete(fileEntity);
            throw e;
        }
        return fileEntity;
    }

    @Transactional
    public FileEntity updateFileStream(FileEntity file, String name, String contentType, Long size, InputStream inputStream) throws IOException {
        checkSpecialCharacters(name);
        file.setName(name);
        file.setSize(size);
        file.setMimeType(contentType);
        file.setLastModification(new Date());
        try {
            fileUtil.storeFile(file.getId(), inputStream);
            fileRepository.save(file);
        } catch (IOException e) {
            throw e;
        }
        return file;
    }
    

    private void checkSpecialCharacters(String name) {
        char[] forbiddenChars = {'<', '>', ':', '"', '/', '\\', '|', '?', '*', '\0'};
        
        for (char c : forbiddenChars) {
            if (name.indexOf(c) != -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Name cannot contain character: " + c);
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
    
    private String checkName(String name, FolderEntity parent, int index, String original) {
        checkSpecialCharacters(name);
        List<FileEntity> folders = fileRepository.findByParentAndName(parent, name);
        String[] split= original.split("\\.");
        if(folders.isEmpty()) return name;
        else {
            if(split.length > 1) {
                return checkName(split[split.length-2] + " (" + index + ")." + split[split.length-1], parent, index + 1, original);
            }
            else {
                return checkName(split[0] + " (" + index + ")", parent, index + 1, original);
            }
        }
    }

    public FileEntity updateFile(UUID fileId, String name, UUID parentId) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(fileId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        
        if(parentId != null && !parentId.equals(file.getParent().getElementId())) {
            FolderEntity newParent = folderRepository.findByElementId(new ElementEntity(parentId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));
            file.setParent(newParent);
        }

        if(name != null && !name.equals("") && !name.equals(file.getName())) {        
            checkSpecialCharacters(name);
            if (fileRepository.findByParentAndName(file.getParent(), name).size() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A file with the name '" + name + "' already exists in this folder");
            }
            file.setName(name);
        }
        file.setLastModification(new Date());
        return fileRepository.save(file);
    }

    public List<UUID> deleteFile(UUID fileId) throws IOException {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(fileId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        UUID elementId = file.getElementId();
        fileRepository.delete(file);
        fileUtil.deleteFile(file.getId());
        return List.of(elementId);
    }

    public List<UUID> deleteAll(List<FileEntity> files) throws IOException {
        List<UUID> ids = new ArrayList<>();
        for (FileEntity file : files) {
            ids.add(file.getElementId());
            fileUtil.deleteFile(file.getId());
        }
        fileRepository.deleteAll(files);
        return ids;
    }

    public void setDeleteFile(UUID fileId, UUID userId) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(fileId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        file.setDeleted(true);
        fileRepository.save(file);
    }

    public List<UUID> setDeletedState(UUID fileId, boolean deleted) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(fileId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if(file.getParent() != null) {
            // Prevent restoring if another non-deleted file with the same name exists in the same folder
            boolean exists = fileRepository.findByParentAndName(file.getParent(), file.getName()).stream()
                    .anyMatch(f -> !f.getId().equals(file.getId()) && !Boolean.TRUE.equals(f.getDeleted()));
            if(exists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A file with the same name already exists in this folder");
            }
        }
        file.setDeleted(deleted);
        fileRepository.save(file);
        return List.of(file.getElementId());
    }

    public void moveFile(UUID fileId, UUID folderId, UUID userId) {
        FileEntity fileEntity = fileRepository.findByElementId(new ElementEntity(fileId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        FolderEntity folder = folderRepository.findByElementId(new ElementEntity(folderId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        fileEntity.setName(checkName(fileEntity.getName(), folder, 1, fileEntity.getName()));

        FolderEntity prevParent = fileEntity.getParent();
        fileEntity.setParent(folder);
        fileEntity.setLastModification(new Date());
        fileRepository.save(fileEntity);
    }

    @Transactional
    public FileEntity copyFile(UUID fileId, UUID newParentFolderId, UUID userId) throws IOException {
        FileEntity originalFile = getFile(fileId);
        FolderEntity newParentFolder = folderRepository.findByElementId(new ElementEntity(newParentFolderId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));

        FileEntity newFile = new FileEntity();
        newFile.setName(checkName(originalFile.getName(), newParentFolder, 1, originalFile.getName()));
        newFile.setSize(originalFile.getSize());
        newFile.setParent(newParentFolder);
        newFile.setUserId(userId);

        ElementEntity id = new ElementEntity();
        id.setFolder(false);
        id = elementRepository.save(id);
        newFile.setElementId(id);

        FileEntity savedFile = fileRepository.save(newFile);
        try {
            fileUtil.copyFile(originalFile.getId(), savedFile.getId());
        } catch (IOException e) {
            fileRepository.delete(savedFile);
            e.printStackTrace();
            throw e;
        }
        fileAccessService.addFileAccess(savedFile.getElementId(), userId, AccessType.ADMIN);

        return savedFile;
    }

    public FileEntity getFile(UUID fileId) {
        return fileRepository.findByElementId(new ElementEntity(fileId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    public FileEntity getFolderByElementId(UUID elementId, boolean deleted) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(elementId)).orElse(null);
        if(file == null || file.getDeleted() != deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return file;
    }

    public FileEntity getFileByElementId(UUID elementId, boolean deleted) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(elementId)).orElse(null);
        if(file == null || file.getDeleted() != deleted) {
            return null;
        }
        return file;
    }

    public void restore(UUID elementId, UUID userId) {
        FileEntity file = fileRepository.findByElementId(new ElementEntity(elementId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getParent() != null && file.getParent().getDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot restore a file whose parent folder is in the trash. Please restore the parent folder first");
        }
        file.setDeleted(false);
        fileRepository.save(file);
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        fileRepository.deleteByUserId(userId);
    }

    public Resource downloadMultipleElementsAsZip(UUID userId, List<UUID> elementIds) {
        List<Object> elementsToZip = new ArrayList<>();
        for (UUID elementId : elementIds) {
            ElementEntity element = elementRepository.findById(elementId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Id " + elementId + " not found"));

            fileAccessService.checkAccessElement(userId, elementId, element.isFolder(), AccessType.READ);

            if (element.isFolder()) {
                FolderEntity folder = folderRepository.findByElementId(element)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder " + elementId + " not found"));
                elementsToZip.add(folder);
            } else {
                FileEntity file = fileRepository.findByElementId(element)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File " + elementId + " not found"));
                elementsToZip.add(file);
            }
        }
        return ZipUtil.createZipFromElements(elementsToZip, fileUtil);
    }

    public String getHash(UUID fileId) {
        return fileUtil.getHash(fileId);
    }
}