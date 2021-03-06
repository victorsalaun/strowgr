/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.core.configuration;

import java.util.HashMap;
import java.util.Map;

import static com.vsct.dt.strowgr.admin.Preconditions.checkStringNotEmpty;

public class IncomingEntryPointBackendServer {

    private final String id;
    private final String hostname;
    private final String ip;
    private final String port;

    private final HashMap<String, String> context;

    public IncomingEntryPointBackendServer(String id, String ip, String port, Map<String, String> context) {
        this.id = checkStringNotEmpty(id, "Backend should have an id");
        this.ip = checkStringNotEmpty(ip, "Backend should have an ip");
        // TODO remove unnecessary hostname attribute
        this.hostname = this.id;
        this.port = checkStringNotEmpty(port, "Backend should have a port");
        this.context = new HashMap<>();
        if (context != null) {
            this.context.putAll(context);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IncomingEntryPointBackendServer that = (IncomingEntryPointBackendServer) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        return context != null ? context.equals(that.context) : that.context == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (ip != null ? ip.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IncommingEntryPointBackendServer{" +
                "id='" + id + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", context=" + context +
                '}';
    }
}
