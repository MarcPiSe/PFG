package edu.udg.tfg.Trash.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.UUID;

public record DeleteElemtnConfirm(@JsonProperty("userId") String userId,
                                  @JsonProperty("elementId") String elementId,
                                  @JsonProperty("service") String service)
        implements Serializable {
}
