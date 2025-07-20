package edu.udg.tfg.SyncService.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "UserManagement")
public interface UserManagementClient {
    
    
    @GetMapping("/users/internal/{username}/id")
    UUID getUserId(@PathVariable("username") String username);
} 