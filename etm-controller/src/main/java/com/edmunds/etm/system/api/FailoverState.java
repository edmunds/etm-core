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

/**
 * Enumerates the ETM system failover states.
 *
 * @author Ryan Holmes
 */
public enum FailoverState {

    /**
     * Controller is actively responding to connected client applications.
     * <p/>
     * An active controller reconfigures the load balancer and deploys new URL rule sets in response to client
     * application connections and disconnections.
     */
    ACTIVE,

    /**
     * Controller operations are suspended.
     * <p/>
     * This is a temporary state intended for maintenance and/or diagnostics. An active controller can be manually
     * suspended and reactivated as desired. A controller in any other state cannot be suspended.
     */
    SUSPENDED,

    /**
     * Controller is on standby, waiting to become active.
     * <p/>
     * A controller in standby state performs no operations. One standby controller will automatically become active
     * when the active controller loses its connection to ZooKeeper.
     */
    STANDBY,

    /**
     * Failover state is unknown.
     * <p/>
     * This is typically used as an initial or fallback value.
     */
    UNKNOWN
}
