package edu.udg.tfg.FileManagement.entities.mappers;

import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.controlllers.responses.FolderInfo;
import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.entities.FolderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = {FileMapper.class})
public interface FolderMapper {

    @Mapping(source = "elementId", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "creationDate", target = "createdAt")
    @Mapping(source = "lastModification", target = "updatedAt")
    @Mapping(source = "parent.elementId", target = "parent")
    @Mapping(target = "type", constant = "folder")
    @Mapping(target = "isDirectory", constant = "true")
    @Mapping(target = "subfolders", source = "children")
    @Mapping(target = "files", source = "files")
    @Mapping(target = "accessLevel", ignore = true)
    @Mapping(target = "clientDeletedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "ownerUserEmail", ignore = true)
    @Mapping(target = "ownerUserName", ignore = true)
    FolderInfo map(FolderEntity source);

    List<FolderInfo> map(List<FolderEntity> source);

    default ElementEntity map(UUID id) {
        if (id == null) {
            return null;
        }
        return new ElementEntity(id);
    }
    default UUID mapParent(FolderEntity parent) {
        return parent != null ? parent.getElementId() : null;
    }
}
