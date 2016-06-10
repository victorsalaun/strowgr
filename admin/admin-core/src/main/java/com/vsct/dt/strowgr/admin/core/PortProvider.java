package com.vsct.dt.strowgr.admin.core;

import java.util.Map;
import java.util.Optional;

/**
 * Provider for ports administration.
 * <p>
 * Created by william_montaz on 26/02/2016.
 */
public interface PortProvider {

    /**
     * Request all ports.
     *
     * @return Optional of a map of all stored ports
     */
    Optional<Map<String, Integer>> getPorts();

    /**
     * Get port for given key.
     *
     * @param key of stored port
     * @return an Optional of the port
     */
    Optional<Integer> getPort(String key);

    default Optional<Integer> getPort(EntryPointKey key, String portId) {
        return getPort(PortProvider.getPortKey(key, portId));
    }

    /**
     * Generate a random port and store it for the given key
     *
     * @param key of the new stored port
     * @return the generated port
     */
    Integer newPort(String key);

    default Integer newPort(EntryPointKey key, String portId) {
        return newPort(PortProvider.getPortKey(key, portId));
    }

    /**
     * Initialize Ports repository structure if not exists.
     *
     * @return Optional of TRUE if ports are initialized, FALSE otherwise
     */
    Optional<Boolean> initPorts();

    /**
     * Construct a port key from an entrypoint key and port id.
     * For instance, with key 'TST/PROD1' and port id 'ADMIN', the portKey is 'TST/PROD1-ADMIN'.
     * This computed key is used as key in the repository.
     *
     * @param key    of the entrypoint key (for ex. 'TST/PROD1')
     * @param portId name of the port type (for ex. 'FRONT', 'SYSLOG', 'ADMIN')
     * @return the port key (for ex. 'TST/PROD1-FRONT')
     */
    static String getPortKey(EntryPointKey key, String portId) {
        return key.getID() + '-' + portId;
    }

}
