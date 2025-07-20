package edu.udg.tfg.UserAuthentication.feignClients.syncService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Component
@FeignClient(name = "${sync.service.microservice.url}")
public interface SyncServiceClient {

    @PostMapping("/sync/root")
    ResponseEntity<Void> addRoot(@RequestHeader("X-User-Id") UUID userId, @RequestParam("elementId") UUID elementId);
} 