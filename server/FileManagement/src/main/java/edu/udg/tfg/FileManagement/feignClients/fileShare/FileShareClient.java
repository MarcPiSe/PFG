package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
   

@Component
@FeignClient(name = "${file.share.microservice.url}")
public interface FileShareClient {

    @PostMapping("/share/internal")
    void shareFileInternal(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") String connectionId, @RequestBody SharedRequest sharedRequest);

    @GetMapping("/{userId}")
    FilesSharedResponse getSharedFiles(@PathVariable("userId") String userId);

    @DeleteMapping("/{fileId}/{userId}")
    void revokeSharedFile(@PathVariable("fileId") String fileId, @PathVariable("userId") String userId);

    @GetMapping("/user/{elementId}")
    UsersSharedResponse getUsersShared(@PathVariable("elementId") UUID elementId);

    @GetMapping("/shares/{userId}/{elementId}")
    SharedInfo getSharedWithUser(@PathVariable("userId") String userId, @PathVariable("elementId") String elementId);
}
