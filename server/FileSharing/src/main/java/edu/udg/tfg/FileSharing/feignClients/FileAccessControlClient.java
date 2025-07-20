package edu.udg.tfg.FileSharing.feignClients;

import edu.udg.tfg.FileSharing.feignClients.responses.AccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import edu.udg.tfg.FileSharing.feignClients.request.AccessRequest;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "FileAccessControl")
public interface FileAccessControlClient {

    @GetMapping("/{fileId}/{userId}")
    AccessResponse getFileAccess(@PathVariable("fileId") UUID fileId, @PathVariable("userId") UUID userId);

    @PostMapping("")
    void createAccess(@RequestBody AccessRequest accessRequest);

    @PostMapping("/internal/list")
    void createAccessList(@RequestBody List<AccessRequest> accessRequest);

    @PutMapping("")
    void modifyAccess(@RequestBody AccessRequest accessRequest);

    @DeleteMapping("/{fileId}/{userId}")
    void deleteAccess(@PathVariable("fileId") UUID fileId, @PathVariable("userId") UUID userId);
} 