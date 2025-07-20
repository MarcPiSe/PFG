package edu.udg.tfg.Trash.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record DeleteAccess(@JsonProperty("userId") String userId,
                           @JsonProperty("elementId") String elementId)
        implements Serializable {
} 