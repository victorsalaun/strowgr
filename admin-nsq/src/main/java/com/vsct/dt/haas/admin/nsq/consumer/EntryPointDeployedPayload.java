package com.vsct.dt.haas.admin.nsq.consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsct.dt.haas.admin.nsq.Payload;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class EntryPointDeployedPayload extends Payload {

    private final String application;
    private final String platform;

    @JsonCreator
    public EntryPointDeployedPayload(@JsonProperty("correlationID") String correlationId,
                                     @JsonProperty("application") String application,
                                     @JsonProperty("platform") String platform) {
        super(correlationId);
        this.application = application;
        this.platform = platform;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }
}
