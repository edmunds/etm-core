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

/**
 * Callback that can implemented to receive a notification whenever the LoadBalancerController activates a new set of
 * vips.
 *
 * The load balancer controller object provides a {@link LoadBalancerController#getActiveVips()} method that retrieves
 * the active load balancer vips in a thread-safe manner. This method should be always be used in preference to storing
 * a copy of the vips, which may become out of synch in a multithreaded environment.
 *
 * @author Ryan Holmes
 */
public interface LoadBalancerControllerCallback {

    public void onActiveVipsUpdated(LoadBalancerController loadBalancerController);
}
