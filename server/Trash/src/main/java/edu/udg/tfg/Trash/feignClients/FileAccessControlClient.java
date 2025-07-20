package edu.udg.tfg.Trash.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;
import edu.udg.tfg.Trash.feignClients.responses.AccessResponse;

@FeignClient(name = "FileAccessControl")
public interface FileAccessControlClient {

    @GetMapping("/{fileId}/{userId}")
    AccessResponse getFileAccess(@PathVariable("fileId") UUID fileId, @PathVariable("userId") UUID userId);
} 