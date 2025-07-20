package edu.udg.tfg.UserManagement.controllers;

import edu.udg.tfg.UserManagement.controllers.responses.InternalUserDetails;
import edu.udg.tfg.UserManagement.services.UserService;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
@RequestMapping("/users/internal")
public class InternalUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{username}/id")
    public ResponseEntity<UUID> getUserIdByUsername(@PathVariable("username") String username) {
        UUID userId = userService.getUserIdByUsername(username);
        return ResponseEntity.ok(userId);
    }

    @GetMapping("/{userId}/details")
    public ResponseEntity<InternalUserDetails> getUserDetails(@PathVariable("userId") UUID userId) {
        InternalUserDetails userDetails = userService.getInternalUserDetails(userId);
        return ResponseEntity.ok(userDetails);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> statusException(ResponseStatusException e) {
        e.printStackTrace();
        return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    }

} 