package edu.udg.tfg.FileSharing.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;

public record DeleteElement(@JsonProperty("userId") String userId,
                            @JsonProperty("elementId") String elementId)
        implements Serializable {
}
