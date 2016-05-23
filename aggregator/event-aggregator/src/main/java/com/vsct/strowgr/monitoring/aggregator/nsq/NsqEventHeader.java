package com.vsct.strowgr.monitoring.aggregator.nsq;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NsqEventHeader {

    private final Date   timestamp;
    private final UUID   correlationId;
    private final String application;
    private final String platform;

    @JsonCreator
    public NsqEventHeader(@JsonProperty("correlationId") UUID correlationId,
                          @JsonProperty("timestamp") Date timestamp,
                          @JsonProperty("application") String application,
                          @JsonProperty("platform") String platform) {
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.application = application;
        this.platform = platform;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getApplication() {
        return application;
    }

    public String getPlatform() {
        return platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NsqEventHeader that = (NsqEventHeader) o;

        if (timestamp != that.timestamp) return false;
        if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null)
            return false;
        if (application != null ? !application.equals(that.application) : that.application != null) return false;
        if (platform != null ? !platform.equals(that.platform) : that.platform != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        result = 31 * result + (application != null ? application.hashCode() : 0);
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NsqPayload{" +
                "timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", application='" + application + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }
}