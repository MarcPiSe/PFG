package edu.udg.tfg.FileManagement.feignClients.fileAccess;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Component
@FeignClient(name = "${file.access.microservice.url}")
public interface FileAccessClient {

    @PostMapping("/")
    void addFileAccess(@RequestBody AccessRequest fileAccess);

    @DeleteMapping("/{fileId}/{userId}")
    void deleteFileAccess(@PathVariable("fileId") String fileId,
                          @PathVariable("userId") String userId);

    @GetMapping("/{fileId}/{userId}")
    AccessResponse getFileAccess(@PathVariable("fileId") String fileId,
                                 @PathVariable("userId") String userId,
                                 @RequestParam(name = "always", required = false, defaultValue = "true") boolean always);
}
