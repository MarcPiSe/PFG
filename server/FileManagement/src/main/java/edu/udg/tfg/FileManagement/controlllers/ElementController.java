package edu.udg.tfg.FileManagement.controlllers;

import com.netflix.discovery.converters.Auto;
import edu.udg.tfg.FileManagement.controlllers.requests.CreateFileRequest;
import edu.udg.tfg.FileManagement.controlllers.requests.SetDeletedRequest;
import edu.udg.tfg.FileManagement.controlllers.requests.UpdateFileRequest;
import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.SetDeletedResponse;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderStructure;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.entities.mappers.FileMapper;
import edu.udg.tfg.FileManagement.entities.mappers.FolderMapper;
import edu.udg.tfg.FileManagement.feignClients.fileAccess.AccessType;
import edu.udg.tfg.FileManagement.services.*;
import edu.udg.tfg.FileManagement.utils.FileUtil;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderInfo;
import org.springframework.web.server.ResponseStatusException;
import edu.udg.tfg.FileManagement.exceptions.CircularDependencyException;
import edu.udg.tfg.FileManagement.exceptions.AccessRuleCreationException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/files")
public class ElementController {

    @Autowired
    private FolderService folderService;
    @Autowired
    private FileService fileService;
    @Autowired
    private ElementService elementService;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private FileAccessService fileAccessService;
    @Autowired
    private FolderMapper folderMapper;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private CommandService commandService;
    @Autowired
    private ElementMappingService elementMappingService;

    @GetMapping("/root")
    public ResponseEntity<?> getRootFolder(@RequestHeader("X-User-Id") UUID userId) {
        try {
            FolderEntity folder = folderService.getRootFolder(userId, false);
            FolderInfo root = folderMapper.map(folder);
            return ResponseEntity.ok(root);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                folderService.createRootFolder(userId);
                FolderEntity folder = folderService.getRootFolder(userId, false);
                FolderInfo root = folderMapper.map(folder);
                return ResponseEntity.ok(root);
            }
            throw e;
        }
    }

    @GetMapping("/structure")
    public ResponseEntity<?> getFolderStructure(@RequestHeader("X-User-Id") UUID userId) {
        FolderStructure structure = folderService.getFolderStructure(userId);
        return ResponseEntity.ok(structure);
    }

    @GetMapping("/{elementId}")
    public ResponseEntity<?> getElement(@RequestHeader("X-User-Id") UUID userId, @PathVariable("elementId") UUID elementId) {
        boolean isFolder = elementService.isFolder(elementId);
        int access = fileAccessService.getFileAccess(elementId, userId, false);
        if (access == 0) {
            throw new ForbiddenException();
        }
        if (isFolder) {
            FolderEntity folder = folderService.getFolderByElementId(elementId, false);
            FolderInfo info = elementMappingService.mapFolderWithDetails(folder, userId);
            info.setAccessLevel(AccessType.values()[access].name());
            return ResponseEntity.ok().body(info);
        } else {
            FileEntity file = fileService.getFolderByElementId(elementId, false);
            FileInfo info = elementMappingService.mapFileWithDetails(file, userId);
            info.setAccessLevel(AccessType.values()[access].name());
            return ResponseEntity.ok().body(info);
        }
    }

    @GetMapping("/internal/folder/{elementId}")
    public ResponseEntity<?> getElementInternal(@PathVariable("elementId") UUID elementId) {
        FolderEntity folder = folderService.getFolderByElementId(elementId, false);
        FolderInfo info = folderMapper.map(folder);
        return ResponseEntity.ok().body(info);
    }

    @GetMapping("/internal/children/{elementId}")
    public ResponseEntity<?> getChildrenInternal(@PathVariable("elementId") UUID elementId) {
        List<UUID> children = folderService.getChildren(elementId);
        return ResponseEntity.ok().body(children);
    }

    @GetMapping(value = "/{elementId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> serveFile(@RequestHeader("X-User-Id") UUID userId, @PathVariable("elementId") UUID elementId) {
        Resource resource;
        String filename;

        boolean isFolder = elementService.isFolder(elementId);
        fileAccessService.checkAccessElement(userId, elementId, isFolder, AccessType.READ);

        if (isFolder) {
            FolderEntity folder = folderService.getFolderByElementId(elementId, false);
            resource = folderService.downloadFolder(elementId);
            filename = folder.getName() + ".zip";
        } else {
            FileEntity fileEntity = fileService.getFile(elementId);
            resource = fileUtil.loadAsResource(fileEntity.getId());
            filename = fileEntity.getName();
        }

        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        long size = -1;
        try {
            size = resource.contentLength();
        } catch (IOException e) {
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        if (size != -1) {
            response.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(size));
        }

        return response.body(resource);
    }

    @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> uploadFileStream(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Connection-Id") UUID connectionId,
            @RequestHeader("parentId") String parentId,
            @RequestHeader("fileName") String fileName,
            @RequestHeader(value = "elementId", required = false) String elementId,
            HttpServletRequest request
    ) throws IOException {
        String dfn = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
        FolderEntity parent = folderService.getFolderByElementId(UUID.fromString(parentId), false);

        if (!fileAccessService.checkAccessFolder(parent, userId, AccessType.WRITE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
        }

        FileEntity fileEntity;

        if (elementId != null && !elementId.isEmpty()) {
            FileEntity existingFile = fileService.getFileByElementId(UUID.fromString(elementId), false);
            if (existingFile != null) {
                if (!fileAccessService.checkAccessFile(userId, existingFile.getElementId(), AccessType.WRITE)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
                }
                fileEntity = fileService.updateFileStream(existingFile, dfn, request.getContentType(), request.getContentLengthLong(), request.getInputStream());
                String selectedConnection = fileEntity.getUserId().equals(userId) ? connectionId.toString() : null;
                commandService.sendUpdate(fileEntity.getElementId(), selectedConnection, fileEntity.getUserId().toString(), fileEntity.getParent(), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");
                return new ResponseEntity<>(fileMapper.map(fileEntity), HttpStatus.OK);
            }
        }

        fileEntity = fileService.createFileFromStream(
                dfn,
                request.getContentType(),
                request.getContentLengthLong(),
                parent.getElementId(),
                userId,
                request.getInputStream(),
                connectionId.toString()
        );
        String selectedConnection = fileEntity.getUserId().equals(userId) ? connectionId.toString() : null;
        commandService.sendCreate(fileEntity.getElementId(), selectedConnection, fileEntity.getUserId().toString(), fileEntity.getParent(), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");

        return new ResponseEntity<>(fileMapper.map(fileEntity), HttpStatus.CREATED);
    }

    @GetMapping("/{folderId}/files")
    public ResponseEntity<?> getFilesByFolder(@RequestHeader("X-User-Id") UUID userId, @PathVariable("folderId") UUID folderId) {
        FolderEntity folder = folderService.getFolderByElementId(folderId, false);
        if (!fileAccessService.checkAccessFolder(folder, userId, AccessType.READ)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
        }

        return ResponseEntity.ok(fileMapper.map(folder.getFiles()));
    }

    @GetMapping("/{folderId}/folders")
    public ResponseEntity<?> getFoldersByFolder(@RequestHeader("X-User-Id") UUID userId, @PathVariable("folderId") UUID folderId) {
        FolderEntity folder = folderService.getFolderByElementId(folderId, false);
        if (!fileAccessService.checkAccessFolder(folder, userId, AccessType.READ)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
        }

        return ResponseEntity.ok(folderMapper.map(folder.getChildren()));
    }

    @GetMapping("/{folderId}/full")
    public ResponseEntity<?> getFolderFullContent(@RequestHeader("X-User-Id") UUID userId, 
                                                @PathVariable("folderId") UUID folderId,
                                                @RequestParam(name = "deleted", defaultValue = "false") boolean deleted) {
        FolderEntity folder = folderService.getFolderByElementId(folderId, deleted);
        if (!fileAccessService.checkAccessFolder(folder, userId, AccessType.READ)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
        }

        FolderInfo info = folderMapper.map(folder);
        folderService.setDetails(info, folderId, userId, deleted, !userId.equals(folder.getUserId()));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/internal/elements/structure")
    public ResponseEntity<FolderInfo> getFolderStructure(@RequestParam("ids") List<UUID> elementIds,
                                                         @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        FolderInfo folderStructure = elementService.getFolderStructure(elementIds, includeDeleted);
        return ResponseEntity.ok(folderStructure);
    }

    @GetMapping("/internal/{elementId}/parent")
    public ResponseEntity<UUID> getParentId(@PathVariable("elementId") UUID elementId) {
        UUID parentId = elementService.getParentId(elementId);
        if (parentId == null) {
            return ResponseEntity.ok().build();         }
        return ResponseEntity.ok(parentId);
    }

    @GetMapping("/internal/{elementId}/parent/element")
    public ResponseEntity<FolderInfo> getParent(@PathVariable("elementId") UUID elementId) {
        FolderEntity el = folderService.getFolderByElementId(elementId, false);
        if (el == null) {
            return ResponseEntity.ok().build();         }
        return ResponseEntity.ok(folderMapper.map(el.getParent()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createElement(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Connection-Id") UUID connectionId,
            @RequestPart("request") CreateFileRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {

        if (request.getContentType() != null && "folder".equalsIgnoreCase(request.getContentType()) || request.isFolder() != null && request.isFolder()) {
            request.setFolder(true);
        } else if (request.isFolder() == null) {
            request.setFolder(false);
        }

        fileAccessService.checkAccessElement(userId, request.getParentFolderId(), true, AccessType.WRITE);

        if (request.isFolder()) {
            FolderEntity folder = folderService.createFolder(
                    request.getName(),
                    folderService.getFolderByElementId(request.getParentFolderId(), false),
                    userId,
                    false,
                    connectionId.toString()
            );
            String selectedConnection = folder.getUserId().equals(userId) ? connectionId.toString() : null;
            commandService.sendCreate(folder.getElementId(), selectedConnection, folder.getUserId().toString(), folder.getParent(), "", folder.getName(), "folder");  
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(folderMapper.map(folder));
        } else {
            if (file == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            FileEntity fileEntity;
            if (request.getElementId() != null) {
                fileEntity = fileService.getFolderByElementId(UUID.fromString(request.getElementId()), false);
                fileEntity.setName(request.getName());
                fileEntity.setMimeType(request.getContentType());
                fileEntity.setSize(request.getSize());
            } else {
                fileEntity = fileService.createFile(
                        request.getName(),
                        request.getContentType(),
                        request.getSize(),
                        request.getParentFolderId(),
                        userId,
                        connectionId.toString()
                );
            }
            
            try {
                fileUtil.storeFile(fileEntity.getId(), file);
            } catch (IOException e) {
                fileService.deleteFile(fileEntity.getElementId());
            }
            String selectedConnection = fileEntity.getUserId().equals(userId) ? connectionId.toString() : null;
            commandService.sendCreate(fileEntity.getElementId(), selectedConnection, fileEntity.getUserId().toString(), fileEntity.getParent(), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");
            return new ResponseEntity<>(fileMapper.map(fileEntity), HttpStatus.CREATED);
        }
    }

    @PostMapping("/root")
    public ResponseEntity<?> createRoot(@RequestHeader("X-User-Id") UUID userId) {
        try {
            FolderEntity rootFolder = folderService.createRootFolder(userId);
            return ResponseEntity.ok(rootFolder.getElementId().toString());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/{elementId}")
    public ResponseEntity<?> updateElement(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @RequestBody UpdateFileRequest request, @PathVariable("elementId") UUID elementId) {
        elementService.elementNotDeleted(elementId);
        
        boolean isFolder = elementService.isFolder(elementId);
        fileAccessService.checkAccessElement(userId, elementId, isFolder, AccessType.WRITE);
        UUID parentId = null;
        if(request.getParentId() != null) {
            parentId = UUID.fromString(request.getParentId());
            fileAccessService.checkAccessElement(userId, parentId, true, AccessType.WRITE);
        } 
        if (isFolder) {
            FolderEntity folder = folderService.updateFolderMetadata(elementId, request.getName(), parentId);
            commandService.sendUpdate(folder.getElementId(), connectionId.toString(), userId.toString(), folder.getParent(), "", folder.getName(), "folder");
        } else {
            FileEntity file = fileService.updateFile(elementId, request.getName(), parentId);
            commandService.sendUpdate(file.getElementId(), connectionId.toString(), userId.toString(), file.getParent(), fileService.getHash(file.getId()), file.getName(), "file");
        }
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{elementId}/move/{folderId}")
    public ResponseEntity<?> moveFolder(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId, @PathVariable("folderId") UUID folderId) {
                elementService.elementNotDeleted(elementId);
        
        boolean isFolder = elementService.isFolder(elementId);
        fileAccessService.checkAccessElement(userId, elementId, isFolder, AccessType.WRITE);
        fileAccessService.checkAccessElement(userId, folderId, true, AccessType.WRITE);

        if (isFolder) {
            folderService.moveFile(elementId, folderId, userId);
            commandService.sendUpdate(elementId, connectionId.toString(), userId.toString(), folderService.getFolderByElementId(folderId, false), "", "", "folder");
        } else {
            fileService.moveFile(elementId, folderId, userId);
            FileEntity fileEntity = fileService.getFile(elementId);
            commandService.sendUpdate(elementId, connectionId.toString(), userId.toString(), folderService.getFolderByElementId(folderId, false), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");
        }

        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{elementId}")
    public ResponseEntity<?> deleteElemnt(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId) {
        boolean isFolder = elementService.isFolder(elementId);
        fileAccessService.checkAccessElement(userId, elementId, isFolder, AccessType.WRITE);

        if (isFolder) {
            FolderEntity folder =folderService.getFolderByElementId(elementId, false);
            folderService.setRemoveFolder(elementId, userId);
            commandService.sendDelete(elementId, connectionId.toString(), userId.toString(), folder.getParent(), "", "", "folder");
        } else {
            FileEntity fileEntity = fileService.getFile(elementId);
            fileService.setDeleteFile(elementId, userId);
            commandService.sendDelete(elementId, connectionId.toString(), userId.toString(), fileEntity.getParent(), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/download/bulk")
    public ResponseEntity<?> downloadFilesBulk(@RequestHeader("X-User-Id") UUID userId,  @RequestBody java.util.List<java.util.UUID> elementIds) {
        Resource zip = fileService.downloadMultipleElementsAsZip(userId, elementIds);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"download.zip\"")
                .body(zip);
    }

    @PostMapping("/{elementId}/copy/{newParentId}")
    public ResponseEntity<?> copyElement(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Connection-Id") UUID connectionId,
            @PathVariable("elementId") UUID elementId,
            @PathVariable("newParentId") UUID newParentId
    ) throws IOException {
                elementService.elementNotDeleted(elementId);
        
        boolean isFolder = elementService.isFolder(elementId);
        fileAccessService.checkAccessElement(userId, elementId, isFolder, AccessType.READ);
        fileAccessService.checkAccessElement(userId, newParentId, true, AccessType.WRITE);

        if (isFolder) {
            folderService.copyFolder(elementId, newParentId, userId, connectionId.toString());
            commandService.sendCreate(elementId, connectionId.toString(), userId.toString(), folderService.getFolderByElementId(newParentId, false), "", "", "folder");
        } else {
            fileService.copyFile(elementId, newParentId, userId);
            FileEntity fileEntity = fileService.getFile(elementId);
            commandService.sendCreate(elementId, connectionId.toString(), userId.toString(), folderService.getFolderByElementId(newParentId, false), fileService.getHash(fileEntity.getId()), fileEntity.getName(), "file");
        }

        return ResponseEntity.accepted().build();
    }

    @PutMapping("/internal/elements/{elementId}/state")
    public ResponseEntity<SetDeletedResponse> setElementDeletedState(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId, @RequestBody SetDeletedRequest request) {
        boolean isFolder = elementService.isFolder(elementId);
        List<UUID> affectedIds;

        if (isFolder) {
            affectedIds = folderService.setDeletedState(elementId, request.getDeleted());
        } else {
            affectedIds = fileService.setDeletedState(elementId, request.getDeleted());
        }
        for(UUID affectedId : affectedIds) {
            if(elementService.isFolder(affectedId)) {
                FolderEntity folder = folderService.getFolderByElementId(affectedId, request.getDeleted());
                if (request.getDeleted()) {
                    commandService.sendDelete(folder.getElementId(), connectionId.toString(), userId.toString(), folder.getParent(), "", folder.getName(), "folder");
                } else {
                    commandService.sendCreate(folder.getElementId(), connectionId.toString(), userId.toString(), folder.getParent(), "", folder.getName(), "folder");
                }
            } else {
                FileEntity file = fileService.getFile(affectedId);
                if (request.getDeleted()) {
                    commandService.sendDelete(file.getElementId(), connectionId.toString(), userId.toString(), file.getParent(), "", file.getName(), "file");
                } else {
                    commandService.sendCreate(file.getElementId(), connectionId.toString(), userId.toString(), file.getParent(), fileService.getHash(file.getId()), file.getName(), "file");
                }
            }
        }
        return ResponseEntity.ok(new SetDeletedResponse(affectedIds));
    }

    @DeleteMapping("/internal/elements/{elementId}/permanent")
    public ResponseEntity<SetDeletedResponse> deleteElementPermanently(@PathVariable("elementId") UUID elementId) throws IOException {
        boolean isFolder = elementService.isFolder(elementId);
        List<UUID> affectedIds;
        if (isFolder) {
            affectedIds = folderService.removeFolder(elementId);
        } else {
            affectedIds = fileService.deleteFile(elementId);
        }
        return ResponseEntity.ok(new SetDeletedResponse(affectedIds));
    }

    @ExceptionHandler(AccessRuleCreationException.class)
    public ResponseEntity<String> handleAccessRuleCreationException(AccessRuleCreationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(CircularDependencyException.class)
    public ResponseEntity<String> handleCircularDependencyException(CircularDependencyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleRuntimeException(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Element not found");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have access to this resource");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> statusException(ResponseStatusException e) {
        e.printStackTrace();
        return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    }
}