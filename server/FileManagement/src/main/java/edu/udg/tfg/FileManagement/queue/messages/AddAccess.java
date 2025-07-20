package edu.udg.tfg.FileManagement.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.UUID;

public record AddAccess(@JsonProperty("userId") String userId,
                        @JsonProperty("elementId") String elementId,
                        @JsonProperty("accessType") int accessType)
        implements Serializable {
}