package edu.udg.tfg.FileManagement.feignClients.UserManagement;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@Component
@FeignClient(name = "${user.management.microservice.url}")
public interface UserManagementClient {

    @GetMapping("/users")
    UserDTO get(@RequestHeader("X-User-Id") UUID id);
}
