package edu.udg.tfg.Trash.controllers;

import edu.udg.tfg.Trash.controllers.requests.TrashRequest;
import edu.udg.tfg.Trash.controllers.responses.TrashDetailsResponse;
import edu.udg.tfg.Trash.controllers.responses.TrashResponse;
import edu.udg.tfg.Trash.entities.mappers.TrashRecordMapper;
import edu.udg.tfg.Trash.services.TrashService;
import edu.udg.tfg.Trash.controllers.responses.FolderInfo;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trash")
public class TrashController {

    @Autowired
    private TrashService trashService;
    
    @Autowired
    TrashRecordMapper trashRecordMapper;

    @PostMapping("/{userId}")
    public ResponseEntity<?> addRecord(@PathVariable("userId") UUID userId, @RequestBody TrashRequest trashRequest){
        trashService.addRecord(userId, trashRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<TrashResponse> getTrashFiles(@PathVariable("userId") UUID userId) {
        TrashResponse trashResponse = trashService.getSharedFiles(userId);
        return ResponseEntity.ok(trashResponse);
    }

    @GetMapping("/{userId}/{elementId}")
    public ResponseEntity<TrashDetailsResponse> getTrashFile(@PathVariable("userId") UUID userId, @PathVariable("elementId") UUID elementId) {
        return ResponseEntity.ok(
                trashService.getTrashFile(userId, elementId)
        );
    }

    @DeleteMapping("/{userId}/{elementId}")
    public ResponseEntity<?> restoreFile(@PathVariable("userId") UUID userId, @PathVariable("elementId") UUID elementId) {
        trashService.remove(userId, elementId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/bulk")
    public ResponseEntity<?> restoreFiles(@PathVariable("userId") UUID userId, @RequestBody List<UUID> elementIds) {
        trashService.removeBulk(userId, elementIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/move/{elementId}")
    public ResponseEntity<?> moveToTrash(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId) {
        try {
            trashService.moveToTrash(userId, connectionId, elementId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{elementId}/restore")
    public ResponseEntity<?> restoreFromTrash(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId) {
        trashService.restoreFromTrash(userId, connectionId, elementId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{elementId}")
    public ResponseEntity<?> deletePermanently(@RequestHeader("X-User-Id") UUID userId, @PathVariable("elementId") UUID elementId) {
        trashService.deletePermanently(userId, elementId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/root")
    public ResponseEntity<FolderInfo> getTrashRoot(@RequestHeader("X-User-Id") UUID userId) {
        try {
            FolderInfo folderInfo = trashService.getTrashRoot(userId);
            return ResponseEntity.ok(folderInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new FolderInfo());
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Element not found");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleGenericRuntimeException(RuntimeException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> statusException(ResponseStatusException e) {
        e.printStackTrace();
        return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    }
}
