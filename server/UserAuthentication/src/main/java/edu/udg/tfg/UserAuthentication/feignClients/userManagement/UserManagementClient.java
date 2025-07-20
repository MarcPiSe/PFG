package edu.udg.tfg.UserAuthentication.feignClients.userManagement;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Component
@FeignClient(name = "${user.management.microservice.url}")
public interface UserManagementClient {

    @PostMapping("/users")
    ResponseEntity<String> creteUser(@RequestHeader("X-User-Id") UUID userId, @RequestBody UserRequest userRequest);

    @DeleteMapping("/users/{userId}")
    ResponseEntity<Void> deleteUser(@PathVariable("userId") UUID userId);

    @GetMapping("/users/check-email")
    ResponseEntity<Boolean> checkEmailAvailability(@RequestParam("email") String email);
}
