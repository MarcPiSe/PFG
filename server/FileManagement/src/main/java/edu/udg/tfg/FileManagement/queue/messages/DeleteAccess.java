package edu.udg.tfg.FileManagement.queue.messages;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeleteAccess(@JsonProperty("userId") String userId,
                           @JsonProperty("elementId") String elementId)
        implements Serializable {
}