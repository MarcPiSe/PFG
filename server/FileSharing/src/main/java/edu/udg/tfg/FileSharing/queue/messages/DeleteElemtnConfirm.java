package edu.udg.tfg.FileSharing.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record DeleteElemtnConfirm(@JsonProperty("userId") String userId,
                                  @JsonProperty("elementId") String elementId,
                                  @JsonProperty("service") String service)
        implements Serializable {
}
