package com.vsct.dt.strowgr.admin.gui.mapping.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HaproxyMappingJson {

    @NotEmpty
    private final String name;
    @NotEmpty
    private final String vip;
    @NotEmpty
    private final String platform;
    @NotNull
    private final Boolean autoreload;

    @JsonCreator
    public HaproxyMappingJson(@JsonProperty("name") String name,
                              @JsonProperty("vip") String vip,
                              @JsonProperty("platform") String platform,
                              @JsonProperty("autoreload") boolean autoreload) {
        this.name = name;
        this.vip = vip;
        this.platform = platform;
        this.autoreload = autoreload;
    }

    public String getName() {
        return name;
    }

    public String getVip() {
        return vip;
    }

    public String getPlatform() {
        return platform;
    }

    public boolean getAutoreload() {
        return autoreload;
    }
}
