package edu.udg.tfg.UserManagement.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "FileSharing")
public interface FileSharingClient {
    
    @DeleteMapping("/internal/shares/user/{userId}")
    ResponseEntity<Void> cleanupSharedAccessForUser(@PathVariable("userId") UUID userId);
} 