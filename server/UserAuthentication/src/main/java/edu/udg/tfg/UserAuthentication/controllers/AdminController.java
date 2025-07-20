package edu.udg.tfg.UserAuthentication.controllers;

import edu.udg.tfg.UserAuthentication.entities.UserEntity;
import edu.udg.tfg.UserAuthentication.services.UserService;
import edu.udg.tfg.UserAuthentication.feignClients.userManagement.UserManagementClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import edu.udg.tfg.UserAuthentication.entities.Roles;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserManagementClient userManagementClient;

    @GetMapping("/users")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(@RequestHeader("X-User-Id") UUID id, @PathVariable("userId") UUID userId, @RequestBody UserEntity userDetails) {
        Optional<UserEntity> user = userService.findById(id);
        Optional<UserEntity> userToUpdate = userService.findById(userId);

        if(user.isEmpty() || userToUpdate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if(user.get().getRole().equals(Roles.SUPER_ADMIN)) {
            userService.updateUserByAdmin(userId, userDetails.getUsername(), userDetails.getRole().name(), userDetails.getPassword());
            return ResponseEntity.ok().build();
        } else if(userToUpdate.get().getRole().equals(Roles.USER) && user.get().getRole().equals(Roles.ADMIN)) {
            userService.updateUser(userId, userDetails.getUsername(), userDetails.getRole().name());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@RequestHeader("X-User-Id") UUID id, @PathVariable("userId") UUID userId) {
        Optional<UserEntity> user = userService.findById(id);
        Optional<UserEntity> userToUpdate = userService.findById(userId);
        if(user.isEmpty() || userToUpdate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if(user.get().getRole().equals(Roles.SUPER_ADMIN) || (userToUpdate.get().getRole().equals(Roles.USER) && user.get().getRole().equals(Roles.ADMIN))) {
            userManagementClient.deleteUser(userId);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> statusException(ResponseStatusException e) {
        e.printStackTrace();
        return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    }

} 