package edu.udg.tfg.FileSharing.controllers;

import edu.udg.tfg.FileSharing.controllers.requests.ShareRequest;
import edu.udg.tfg.FileSharing.controllers.requests.UpdateShareRequestDTO;
import edu.udg.tfg.FileSharing.controllers.responses.SharedInfo;
import edu.udg.tfg.FileSharing.feignClients.responses.FolderInfo;
import edu.udg.tfg.FileSharing.services.FileSharingService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import edu.udg.tfg.FileSharing.services.CommandService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class FileSharingController {

    @Autowired
    private FileSharingService fileSharingService;

    @PostMapping("/share")
    public ResponseEntity<?> shareElement(@RequestHeader("X-User-Id") UUID requesterId,
                                          @RequestHeader("X-Connection-Id") String connectionId,
                                        @RequestBody ShareRequest request) {
        fileSharingService.shareElement(requesterId, request, connectionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/share/internal")
    public ResponseEntity<?> shareElementInternal(@RequestHeader("X-User-Id") UUID requesterId, 
                                                @RequestHeader("X-Connection-Id") String connectionId,
                                        @RequestBody ShareRequest request) {
        fileSharingService.shareElementInternal(requesterId, request, connectionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/share/root")
    public ResponseEntity<FolderInfo> getSharedWithUserRoot(@RequestHeader("X-User-Id") UUID userId) {
        FolderInfo sharedRoot = fileSharingService.getSharedWithUserRoot(userId);
        return ResponseEntity.ok(sharedRoot);
    }

    @GetMapping("/shares/user/{elementId}")
    public ResponseEntity<List<SharedInfo>> getUsersSharedWithElement(@PathVariable("elementId") String elementId) {
        List<SharedInfo> sharedUsers = fileSharingService.getUsersSharedWithElement(UUID.fromString(elementId));
        return ResponseEntity.ok(sharedUsers);
    }

    @GetMapping("/shares/{userId}/{elementId}")
    public ResponseEntity<SharedInfo> getSharedWithUser(@PathVariable("userId") String userId, @PathVariable("elementId") String elementId) {
        SharedInfo sharedInfo = fileSharingService.getSharedWithUserAndElementId(UUID.fromString(userId), UUID.fromString(elementId));
        return ResponseEntity.ok(sharedInfo);
    }

    @PutMapping("/share")
    public ResponseEntity<?> updateShareAccess(@RequestHeader("X-User-Id") UUID requesterId, 
                                             @RequestBody UpdateShareRequestDTO request) {
        fileSharingService.updateShareAccess(requesterId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/shares/{elementId}/users/{username}")
    public ResponseEntity<Void> revokeShare(@RequestHeader("X-User-Id") UUID requesterId, 
                                            @RequestHeader("X-Connection-Id") String connectionId,
                                          @PathVariable("elementId") String elementId, 
                                          @PathVariable("username") String username) {
        fileSharingService.revokeShare(requesterId, UUID.fromString(elementId), username, connectionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/shares/user/{userId}")
    public ResponseEntity<Void> cleanupSharedAccessForUser(@PathVariable("userId") String userId, @RequestHeader("X-Connection-Id") String connectionId) {
        fileSharingService.deleteByUserId(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
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