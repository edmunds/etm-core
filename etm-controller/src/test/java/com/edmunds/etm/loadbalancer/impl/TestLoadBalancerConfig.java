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
package com.edmunds.etm.loadbalancer.impl;

import com.edmunds.etm.loadbalancer.api.LoadBalancerConfig;

/**
 * Simple load balancer configuration used for unit testing.
 *
 * @author David Trott
 */
public class TestLoadBalancerConfig implements LoadBalancerConfig {
    @Override
    public String getIpPoolStart() {
        return "1.2.3.4";
    }

    @Override
    public String getIpPoolEnd() {
        return "1.2.3.6";
    }

    @Override
    public int getDefaultVipPort() {
        return 7000;
    }
}
