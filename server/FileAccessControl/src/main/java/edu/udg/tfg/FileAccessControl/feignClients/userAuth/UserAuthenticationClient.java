package edu.udg.tfg.FileAccessControl.feignClients.userAuth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Component
@FeignClient(name = "${user.auth.microservice.url}")
public interface UserAuthenticationClient {

    @GetMapping("/users/auth/username")
    String getUserName(@RequestHeader("X-User-Id") UUID id);
}
