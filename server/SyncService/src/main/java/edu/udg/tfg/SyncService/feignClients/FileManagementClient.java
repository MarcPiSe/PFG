package edu.udg.tfg.SyncService.feignClients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "FileManagement")
public interface FileManagementClient {

    @GetMapping("/files/internal/{elementId}/parent")
    UUID getParentId(@PathVariable("elementId") UUID elementId);
} 