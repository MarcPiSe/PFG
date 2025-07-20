package edu.udg.tfg.SyncService.feignClients.fileShare;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
   

@Component
@FeignClient(name = "${file.share.microservice.url}")
public interface FileShareClient {

    @GetMapping("/shares/user/{elementId}")
    List<SharedInfo> getUsersShared(@PathVariable("elementId") UUID elementId);
}
