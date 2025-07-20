package edu.udg.tfg.FileManagement.queue.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;

public record CommandRabbit(@JsonProperty("action") String action,
                            @JsonProperty("userId") String userId,
                            @JsonProperty("elementId") String elementId,
                            @JsonProperty("hash") String hash,
                            @JsonProperty("path") String path,
                            @JsonProperty("name") String name,
                            @JsonProperty("type") String type,
                            @JsonProperty("connectionId") String connectionId,
                            @JsonProperty("parentId") String parentId)
        implements Serializable {
}

