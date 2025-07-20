package edu.udg.tfg.FileManagement.feignClients.trash;

import edu.udg.tfg.FileManagement.feignClients.fileShare.SharedRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Component
@FeignClient(name = "${file.trash.microservice.url}")
public interface TrashClient {

    @GetMapping("/{userId}")
    TrashResponse getTrashFiles(@PathVariable("userId") String userId);

    @PostMapping("/{userId}")
    void addRecord(@PathVariable("userId") String userId, @RequestBody TrashRequest trashRequest);

    @DeleteMapping("/{userId}/{elementId}")
    ResponseEntity<?> restoreFile(@PathVariable("userId") String userId, @PathVariable("elementId") String elementId);

    @GetMapping("/{userId}/{elementId}")
    TrashDetailsResponse getTrashFile(@PathVariable("userId") UUID userId, @PathVariable("elementId") UUID elementId);

    @DeleteMapping("/{userId}/bulk")
    ResponseEntity<?> restoreFiles(@PathVariable("userId") UUID userId, @RequestBody List<UUID> elementIds);
}
