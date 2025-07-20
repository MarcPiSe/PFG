package edu.udg.tfg.FileSharing.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import edu.udg.tfg.FileSharing.feignClients.responses.FolderInfo;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "FileManagement")
public interface FileManagementClient {
    
    @GetMapping("/files/internal/elements/structure")
    FolderInfo getFolderStructure(@RequestParam("ids") List<UUID> elementIds, 
                                   @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted);

    @GetMapping("/files/internal/{elementId}/parent")
    UUID getParentId(@PathVariable("elementId") UUID elementId);

    @GetMapping("/files/internal/{elementId}/parent/element")
    FolderInfo getParent(@PathVariable("elementId") UUID elementId);

    @GetMapping("/files/internal/folder/{elementId}")
    FolderInfo getFolder(@PathVariable("elementId") UUID elementId);

    @GetMapping("/files/internal/children/{elementId}")
    List<UUID> getChildren(@PathVariable("elementId") UUID elementId);
} 