package edu.udg.tfg.Trash.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import edu.udg.tfg.Trash.feignClients.requests.SetDeletedRequest;
import edu.udg.tfg.Trash.controllers.responses.FolderInfo;
import edu.udg.tfg.Trash.feignClients.responses.SetDeletedResponse;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "${file.management.microservice.url}", path = "/files")
public interface FileManagementClient {

    @PutMapping("/internal/elements/{elementId}/state")
    SetDeletedResponse setElementDeletedState(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Connection-Id") UUID connectionId, @PathVariable("elementId") UUID elementId, @RequestBody SetDeletedRequest request);

    @DeleteMapping("/internal/elements/{elementId}/permanent")
    SetDeletedResponse deleteElementPermanently(@PathVariable("elementId") UUID elementId);

    @GetMapping("/internal/elements/structure")
    FolderInfo getFolderStructure(@RequestParam("ids") List<UUID> elementIds, @RequestParam(name = "includeDeleted", defaultValue = "true") boolean includeDeleted);
} 