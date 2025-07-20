package edu.udg.tfg.SyncService.controllers;

import edu.udg.tfg.SyncService.services.SnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sync")
public class SnapshotController {

    @Autowired
    private SnapshotService snapshotService;

    @PostMapping("/root")
    public ResponseEntity<Void> addRoot(@RequestHeader("X-User-Id") UUID userId, 
                                       @RequestParam("elementId") UUID elementId) {
        try {
            snapshotService.addRoot(userId, elementId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 