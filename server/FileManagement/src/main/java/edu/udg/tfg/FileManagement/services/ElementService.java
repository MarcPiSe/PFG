package edu.udg.tfg.FileManagement.services;


import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.repositories.ElementRepository;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import edu.udg.tfg.FileManagement.entities.mappers.FileMapper;
import edu.udg.tfg.FileManagement.entities.mappers.FolderMapper;


@Service
public class ElementService {

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private FileMapper fileMapper;

    public Boolean isFolder(UUID elementId) {
        Optional<ElementEntity> optional = elementRepository.findById(elementId);
        return optional.map(ElementEntity::isFolder).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found"));
    }

    public UUID getParentId(UUID elementId) {
        try {
            if (isFolder(elementId)) {
                FolderEntity folder = folderService.getFolderByElementId(elementId, false);
                return folder.getParent() != null ? folder.getParent().getElementId() : null;
            } else {
                FileEntity file = fileService.getFolderByElementId(elementId, false);
                return file.getParent() != null ? file.getParent().getElementId() : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void elementNotDeleted(UUID elementId) {
        try {
            if (isFolder(elementId)) {
                FolderEntity folder = folderService.getFolderByElementId(elementId, false); 
                if (folder.getDeleted() != null && folder.getDeleted()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element has been deleted");
                }
            } else {
                FileEntity file = fileService.getFolderByElementId(elementId, false); 
                if (file.getDeleted() != null && file.getDeleted()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element has been deleted");
                }
            }
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found");
        }
    }

    public FolderInfo getFolderStructure(List<UUID> elementIds) {
        return getFolderStructure(elementIds, false);
    }

    public FolderInfo getFolderStructure(List<UUID> elementIds, boolean includeDeleted) {
        if (elementIds == null || elementIds.isEmpty()) {
            return new FolderInfo();
        }

        Map<UUID, Object> dtoMap = getElements(elementIds, includeDeleted);
        return orderFolders(dtoMap);
    }

    private Map<UUID, Object> getElements(List<UUID> elementIds, boolean includeDeleted) {
        return elementIds.stream()
                .map(id -> {
                    try {
                        if (isFolder(id)) {
                            return folderMapper.map(folderService.getFolderByElementId(id, includeDeleted));
                        } else {
                            return fileMapper.map(fileService.getFolderByElementId(id, includeDeleted));
                        }
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(
                        dto -> {
                            if (dto instanceof FolderInfo) return ((FolderInfo) dto).getId();
                            return ((FileInfo) dto).getId();
                        },
                        dto -> dto
                ));
    }

    private FolderInfo orderFolders(Map<UUID, Object> elementMap) {
        Map<UUID, FolderInfo> folderDtoMap = elementMap.values().stream()
                .filter(FolderInfo.class::isInstance)
                .map(FolderInfo.class::cast)
                .collect(Collectors.toMap(FolderInfo::getId, f -> f));

        FolderInfo rootResponse = new FolderInfo();

        elementMap.values().forEach(dto -> {
            UUID parentId = getParentId(dto);

            if (parentId != null && folderDtoMap.containsKey(parentId)) {
                FolderInfo parentDto = folderDtoMap.get(parentId);
                if (dto instanceof FolderInfo) {
                    parentDto.getSubfolders().add((FolderInfo) dto);
                } else {
                    parentDto.getFiles().add((FileInfo) dto);
                }
            } else {
                if (dto instanceof FolderInfo) {
                    rootResponse.getSubfolders().add((FolderInfo) dto);
                } else {
                    rootResponse.getFiles().add((FileInfo) dto);
                }
            }
        });

        return rootResponse;
    }

    private UUID getParentId(Object elm) {
        if (elm instanceof FolderInfo) {
            return ((FolderInfo) elm).getParent();
        }
        if (elm instanceof FileInfo) {
            return ((FileInfo) elm).getParent();
        }
        return null;
    }
}