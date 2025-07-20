package edu.udg.tfg.FileManagement.entities.mappers;

import edu.udg.tfg.FileManagement.controlllers.responses.FileInfo;
import edu.udg.tfg.FileManagement.entities.ElementEntity;
import edu.udg.tfg.FileManagement.entities.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface FileMapper {

    List<FileInfo> map(List<FileEntity> destination);


    @Mapping(source = "elementId", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "creationDate", target = "createdAt")
    @Mapping(source = "lastModification", target = "updatedAt")
    @Mapping(source = "parent.elementId", target = "parent")
    @Mapping(target = "type", constant = "file")
    @Mapping(source = "size", target = "size")
    @Mapping(target = "isDirectory", constant = "false")
    @Mapping(target = "accessLevel", ignore = true)
    @Mapping(target = "clientDeletedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "ownerUserEmail", ignore = true)
    @Mapping(target = "ownerUserName", ignore = true)
    FileInfo map(FileEntity source);


    default ElementEntity map(UUID id) {
        if (id == null) {
            return null;
        }
        return new ElementEntity(id);
    }


}
