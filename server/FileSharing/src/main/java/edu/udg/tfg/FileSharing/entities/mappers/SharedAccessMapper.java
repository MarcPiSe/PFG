package edu.udg.tfg.FileSharing.entities.mappers;

import edu.udg.tfg.FileSharing.controllers.responses.FilesSharedResponse;
import edu.udg.tfg.FileSharing.controllers.responses.UsersSharedResponse;
import edu.udg.tfg.FileSharing.entities.SharedAccess;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public class SharedAccessMapper {

    public FilesSharedResponse map(List<SharedAccess> sharedAccesses) {
        FilesSharedResponse filesSharedResponse = new FilesSharedResponse();
        for(SharedAccess sharedAccess : sharedAccesses) {
            filesSharedResponse.getFiles().add(
                    sharedAccess.getElementId()
            );
        }
        return filesSharedResponse;
    }
    public UsersSharedResponse mapUsers(List<SharedAccess> sharedAccesses) {
        UsersSharedResponse filesSharedResponse = new UsersSharedResponse();
        for(SharedAccess sharedAccess : sharedAccesses) {
            filesSharedResponse.getUsers().add(
                    sharedAccess.getElementId()
            );
        }
        return filesSharedResponse;
    }
}
