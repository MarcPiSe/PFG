package edu.udg.tfg.UserManagement.controllers;

import edu.udg.tfg.UserManagement.controllers.responses.UserDetails;
import edu.udg.tfg.UserManagement.services.AdminService;
import edu.udg.tfg.UserManagement.services.UserDeletionService;
import edu.udg.tfg.UserManagement.services.UserService;
import edu.udg.tfg.UserManagement.entities.UserInfo;
import edu.udg.tfg.UserManagement.feignClients.userAuth.UserAuthClient;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.Roles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserDeletionService userDeletionService;
    @Autowired
    private UserAuthClient userAuthClient;

    @GetMapping("/users")
    public ResponseEntity<List<UserDetails>> getAllUsers(@RequestHeader("X-User-Id") UUID id) {
        Roles role = userAuthClient.getUserAuthDetails(id).getRole();
        return ResponseEntity.ok(adminService.getAllUsers(role.equals(Roles.SUPER_ADMIN)));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserDetails> getUser(@PathVariable("username") String username) {
        return ResponseEntity.ok(adminService.getUser(username));
    }

    @PutMapping("/users/{username}")
    public ResponseEntity<Void> updateUser(@RequestHeader("X-User-Id") UUID id, @PathVariable("username") String username, @RequestBody UserDetails userDTO) {
        UUID userId = userService.getUserIdByUsername(username);
        Roles toUpdate = userAuthClient.getUserAuthDetails(userId).getRole();
        Roles current = userAuthClient.getUserAuthDetails(id).getRole();

        if(current.equals(Roles.SUPER_ADMIN) || (toUpdate.equals(Roles.USER) && current.equals(Roles.ADMIN))) {
            if(toUpdate.equals(Roles.SUPER_ADMIN) && !userDTO.getRole().equals(Roles.SUPER_ADMIN.name())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            adminService.updateUser(id, userId, userDTO);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@RequestHeader("X-User-Id") UUID id, @PathVariable("username") String username) {
        UUID userId = userService.getUserIdByUsername(username);
        Roles toDelete = userAuthClient.getUserAuthDetails(userId).getRole();
        Roles current = userAuthClient.getUserAuthDetails(id).getRole();

        if(!toDelete.equals(Roles.SUPER_ADMIN) && (current.equals(Roles.SUPER_ADMIN) || (current.equals(Roles.ADMIN) && toDelete.equals(Roles.USER)))) {
            userDeletionService.startUserDeletion(userId);
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