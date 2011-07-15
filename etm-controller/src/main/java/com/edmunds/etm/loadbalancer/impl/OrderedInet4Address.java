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

import org.apache.commons.lang.Validate;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Represents an IPv4 Address a.b.c.d.
 * <p/>
 * Lightweight class that wraps the 4 byte value (no hostname lookup).
 * Provide methods to compare the order of the address.
 */
public class OrderedInet4Address implements Comparable<OrderedInet4Address> {

    private final byte[] address;

    private static InetAddress parseAddress(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to construct OrderedInet4Address", e);
        }
    }

    public OrderedInet4Address(String address) {
        this(parseAddress(address));
    }

    public OrderedInet4Address(InetAddress inetAddress) {
        this(inetAddress.getAddress());
    }

    public OrderedInet4Address(byte[] address) {
        Validate.notNull(address);
        Validate.isTrue(address.length == 4);
        this.address = address;
    }

    public Inet4Address toInet4Address() {
        try {
            return (Inet4Address) InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            // Never Happen!
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return toInet4Address().getHostAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrderedInet4Address that = (OrderedInet4Address) o;

        if (!Arrays.equals(address, that.address)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    @Override
    public int compareTo(OrderedInet4Address other) {
        Validate.notNull(other);

        for (int i = 0; i < address.length; i++) {
            if (address[i] != other.address[i]) {
                // Do the arithmetic using integers rather than bytes.
                return (address[i] & 0xff) - (other.address[i] & 0xff);
            }
        }
        return 0;
    }

    public OrderedInet4Address getNextAddress() {
        byte[] nextAddress = Arrays.copyOf(address, 4);

        nextAddress[3]++;

        for (int i = 3; i > 0; i--) {
            // Did we overflow?
            if (nextAddress[i] == 0) {
                // Yes do increment the next digit
                nextAddress[i - 1]++;
            } else {
                // No so we can just break out of the loop.
                break;
            }
        }

        return new OrderedInet4Address(nextAddress);
    }
}
