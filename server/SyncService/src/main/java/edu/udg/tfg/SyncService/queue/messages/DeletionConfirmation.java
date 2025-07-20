package edu.udg.tfg.SyncService.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
 
public record DeletionConfirmation(
        @JsonProperty("userId") String userId,
        @JsonProperty("serviceName") String serviceName)
        implements Serializable {
} 