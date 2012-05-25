/*
 * Copyright 2011 Edmunds.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.edmunds.etm.system.api;

import com.edmunds.etm.common.thrift.ControllerInstanceDto;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents an instance of an ETM controller process.
 *
 * @author Ryan Holmes
 */
public class ControllerInstance {

    private static final Logger logger = Logger.getLogger(ControllerInstance.class);

    private final UUID id;
    private final String ipAddress;
    private final String version;
    private FailoverState failoverState;

    private String hostName;

    public ControllerInstance(UUID id,
                              String ipAddress,
                              String version,
                              FailoverState failoverState) {
        Validate.notNull(id, "Unique ID is null");
        Validate.notEmpty(ipAddress, "IP address is empty");
        Validate.notEmpty(version, "Version is empty");
        Validate.notNull(failoverState, "Failover state is null");
        this.id = id;
        this.ipAddress = ipAddress;
        this.version = version;
        this.failoverState = failoverState;
    }

    /**
     * Gets the unique controller identifier.
     *
     * @return unique controller id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the host IP address.
     *
     * @return host IP address
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Gets the host name.
     *
     * @return host name
     */
    public String getHostName() {
        if (hostName == null) {
            try {
                InetAddress addr = InetAddress.getByName(ipAddress);
                hostName = addr.getHostName();
            } catch (UnknownHostException e) {
                return "Unknown";
            }
        }
        return hostName;
    }

    /**
     * Gets the application version.
     *
     * @return application version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the controller failover state.
     *
     * @return failover state
     */
    public FailoverState getFailoverState() {
        return failoverState;
    }

    /**
     * Sets the controller failover state.
     *
     * @param failoverState failover state
     */
    public void setFailoverState(FailoverState failoverState) {
        this.failoverState = failoverState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ControllerInstance)) {
            return false;
        }

        ControllerInstance that = (ControllerInstance) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ControllerInstance");
        sb.append("{id=").append(id);
        sb.append(", ipAddress='").append(ipAddress).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", failoverState=").append(failoverState);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a ControllerInstance from the specified DTO.
     *
     * @param dto the DTO to read
     * @return an ControllerInstance object
     */
    public static ControllerInstance readDto(ControllerInstanceDto dto) {
        if (dto == null) {
            return null;
        }

        UUID id = null;
        try {
            id = UUID.fromString(dto.getId());
        } catch (IllegalArgumentException e) {
            logger.error(String.format("Cannot parse UUID from dto: %s", dto), e);
            return null;
        }

        String ipAddress = dto.getIpAddress();
        String version = dto.getVersion();

        FailoverState failoverState;
        try {
            failoverState = FailoverState.valueOf(dto.getFailoverState());
        } catch (RuntimeException e) {
            logger.error("Invalid failover state read from DTO", e);
            failoverState = FailoverState.UNKNOWN;
        }

        ControllerInstance obj = null;
        try {
            obj = new ControllerInstance(id, ipAddress, version, failoverState);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid controller instance DTO", e);
        }
        return obj;
    }

    /**
     * Creates a DTO from the specified ControllerInstance object.
     *
     * @param obj the ControllerInstance to write
     * @return a data transfer object
     */
    public static ControllerInstanceDto writeDto(ControllerInstance obj) {
        if (obj == null) {
            return null;
        }

        ControllerInstanceDto dto = new ControllerInstanceDto();
        dto.setId(obj.getId().toString());
        dto.setIpAddress(obj.getIpAddress());
        dto.setVersion(obj.getVersion());
        dto.setFailoverState(obj.getFailoverState().name());
        return dto;
    }
}
