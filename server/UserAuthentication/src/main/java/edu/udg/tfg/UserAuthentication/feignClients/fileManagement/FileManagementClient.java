package edu.udg.tfg.UserAuthentication.feignClients.fileManagement;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.UUID;

@Component
@FeignClient(name = "${file.management.microservice.url}")
public interface FileManagementClient {

    @PostMapping("/files/root")
    String createRoot(@RequestHeader("X-User-Id") UUID userId);
}
