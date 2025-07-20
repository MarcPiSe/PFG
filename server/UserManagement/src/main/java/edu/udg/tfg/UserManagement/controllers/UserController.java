package edu.udg.tfg.UserManagement.controllers;

import edu.udg.tfg.UserManagement.controllers.requests.UpdateProfileRequest;
import edu.udg.tfg.UserManagement.controllers.requests.UserRequest;
import edu.udg.tfg.UserManagement.entities.UserInfo;
import edu.udg.tfg.UserManagement.feignClients.userAuth.UserAuthClient;
import edu.udg.tfg.UserManagement.services.UserDeletionService;
import edu.udg.tfg.UserManagement.services.UserService;
import jakarta.validation.Valid;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.Roles;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserDeletionService userDeletionService;
    @Autowired
    private UserAuthClient userAuthClient;

    @PostMapping("")
    public ResponseEntity<?> registerUser(@RequestHeader("X-User-Id") UUID userId, @RequestBody UserRequest userRequest) {
        if (userService.findByEmail(userRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email is already taken");
        }
        UserInfo userInfo = createUser(userRequest);
        userInfo.setId(userId);

        UserInfo newUserInfo = userService.createUser(userInfo);
        return ResponseEntity.ok(newUserInfo);
    }

    private UserInfo createUser(UserRequest userRequest) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userRequest.getId());
        userInfo.setEmail(userRequest.getEmail());
        userInfo.setLastName(userRequest.getLastName());
        userInfo.setCreatedDate(userRequest.getCreatedDate());
        userInfo.setLastModifiedDate(userRequest.getLastModifiedDate());
        userInfo.setFirstName(userRequest.getFirstName());

        return userInfo;
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailAvailability(@RequestParam("email") String email) {
        boolean isAvailable = userService.findByEmail(email).isEmpty();
        return ResponseEntity.ok(isAvailable);
    }

    @GetMapping("")
    public ResponseEntity<?> getUser(@RequestHeader("X-User-Id") UUID id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @DeleteMapping("")
    public ResponseEntity<?> deleteUser(@RequestHeader("X-User-Id") UUID id) {
        Roles toDelete = userAuthClient.getUserAuthDetails(id).getRole();
        if(toDelete.equals(Roles.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userDeletionService.startUserDeletion(id);
        return ResponseEntity.ok().build(); 
    }

    @PutMapping("")
    public ResponseEntity<?> updateUser(@RequestHeader("X-User-Id") UUID id, @RequestBody UserRequest userRequest) {
        UserInfo updatedUserInfo = userService.updateUser(id, userRequest);
        return ResponseEntity.ok(updatedUserInfo);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestHeader("X-User-Id") UUID id, @RequestBody @Valid UpdateProfileRequest updateRequest) {
        UserInfo updatedUserInfo = userService.updateUserProfile(id, updateRequest);
        return ResponseEntity.ok(updatedUserInfo);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam("q") String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
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