package edu.udg.tfg.UserManagement.feignClients.userAuth;

import edu.udg.tfg.UserManagement.controllers.responses.UserAuthDetails;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.UserAuthInfoListResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Component
@FeignClient(name = "${user.auth.microservice.url}")
public interface UserAuthClient {

    @PostMapping("/users/auth/usernames")
    UserAuthInfoListResponse getUsernamesByIds(@RequestBody List<UUID> userIds);

    @GetMapping("/users/auth/internal/search-ids-by-username")
    List<UUID> searchUserIdsByUsername(@RequestParam("q") String query);

    @GetMapping("/users/auth/internal/users/{userId}")
    UserAuthDetails getUserAuthDetails(@PathVariable("userId") UUID userId);

    @GetMapping("/admin/users")
    List<UserAuthDetails> getAllUsers();

    @PutMapping("/admin/users/{userId}")
    void updateUser(@RequestHeader("X-User-Id") UUID id, @PathVariable("userId") UUID userId, @RequestBody UserAuthDetails userDetails);

    @DeleteMapping("/users/auth/internal/{userId}")
    void deleteAccountInternal(@PathVariable("userId") UUID userId);
} 