package edu.udg.tfg.FileAccessControl.controllers;
import edu.udg.tfg.FileAccessControl.controllers.requests.AccessRequest;
import edu.udg.tfg.FileAccessControl.controllers.responses.AccessResponse;
import edu.udg.tfg.FileAccessControl.entities.AccessRule;
import edu.udg.tfg.FileAccessControl.entities.AccessType;
import edu.udg.tfg.FileAccessControl.entities.mappers.AccessMapper;
import edu.udg.tfg.FileAccessControl.services.FileAccessService;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;


@RestController
@RequestMapping
public class FileAccessController {

    @Autowired
    private FileAccessService fileAccessService;

    @Autowired
    private AccessMapper accessMapper;

    @PostMapping("/")
    public ResponseEntity<?> addFileAccess(@RequestBody AccessRequest fileAccess) {
        try {
            if (fileAccess == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRequest cannot be null");
            }
            fileAccessService.addFileAccess(accessMapper.map(fileAccess));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    @PostMapping("/internal/list")
    public ResponseEntity<?> addFileAccessList(@RequestBody List<AccessRequest> fileAccess) {
        try {
            if (fileAccess == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRequest cannot be null");
            }
            fileAccessService.addFileAccessList(accessMapper.mapList(fileAccess));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    @PutMapping("/")
    public ResponseEntity<?> modifyFileAccess(@RequestBody AccessRequest fileAccess) {
        try {
            if (fileAccess == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccessRequest cannot be null");
            }
            fileAccessService.updateAccess(accessMapper.map(fileAccess));
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    @DeleteMapping("/{fileId}/{userId}")
    public ResponseEntity<?> deleteFileAccess(@PathVariable("fileId") UUID fileId, @PathVariable("userId") UUID userId) {
        try {
            if (fileId == null || userId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileId and userId are required and cannot be null");
            }
            fileAccessService.deleteFileAccess(fileId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    @GetMapping("/{fileId}/{userId}")
    public ResponseEntity<?> getFileAccess(@PathVariable("fileId") UUID fileId, @PathVariable("userId") UUID userId, @RequestParam(name = "always", required = false, defaultValue = "true") boolean always) {
        try {
            if (fileId == null || userId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileId and userId are required and cannot be null");
            }
            
            AccessResponse accessRule = accessMapper.map(fileAccessService.getFileAccess(fileId, userId, always));
            return ResponseEntity.ok(accessRule);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data");
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Element not found");
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have access to this resource");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
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
