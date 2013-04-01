package com.edmunds.etm.loadbalancer.impl;

import com.edmunds.common.configuration.api.EnvironmentConfiguration;
import com.edmunds.etm.loadbalancer.api.LoadBalancerConfig;
import com.edmunds.etm.loadbalancer.api.LoadBalancerConnection;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.loadbalancer.api.VirtualServerNotFoundException;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.ManagementVip;
import com.edmunds.etm.management.api.ManagementVipType;
import com.edmunds.etm.management.api.ManagementVips;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;

import static org.easymock.EasyMock.expect;

@Test
public class LoadBalancerControllerTest {
    @Test
    public void verifyOnlyMatchingVipsAreDeleted() throws RemoteException, VirtualServerNotFoundException {
        final VirtualServer badVip = new VirtualServer("etm_edmunds_dev_blah", new HostAddress("1.2.3.4", 1234));

        final Set<VirtualServer> virtualServers = Sets.newHashSet();
        virtualServers.add(badVip);
        virtualServers.add(new VirtualServer("etm_edmunds_qa_blah", new HostAddress("1.2.3.4", 1234)));
        virtualServers.add(new VirtualServer("etm", new HostAddress("1.2.3.4", 1234)));

        final IMocksControl control = EasyMock.createControl();

        final LoadBalancerConnection loadBalancerConnection = control.createMock("loadBalancerConnection", LoadBalancerConnection.class);
        final LoadBalancerConfig loadBalancerConfig = control.createMock("loadBalancerConfig", LoadBalancerConfig.class);
        final EnvironmentConfiguration environmentConfiguration = control.createMock("environmentConfiguration", EnvironmentConfiguration.class);

        expect(environmentConfiguration.getSite()).andStubReturn("edmunds");
        expect(environmentConfiguration.getEnvironmentName()).andStubReturn("dev");

        expect(loadBalancerConnection.connect()).andReturn(Boolean.TRUE);
        expect(loadBalancerConnection.isActive()).andReturn(Boolean.TRUE);
        expect(loadBalancerConnection.saveConfiguration()).andReturn(Boolean.TRUE);

        expect(loadBalancerConnection.getAllVirtualServers()).andReturn(virtualServers);

        // This is the main call we are looking for (and no other calls).
        loadBalancerConnection.deleteVirtualServer(badVip);

        control.replay();

        final LoadBalancerController controller =
                new LoadBalancerController(loadBalancerConnection, loadBalancerConfig, environmentConfiguration);

        final Collection<ManagementVip> emptyVips = Lists.newArrayList();
        final ManagementVips deltaVips = new ManagementVips(ManagementVipType.COMPLETE, emptyVips);
        controller.updateLoadBalancerConfiguration(deltaVips, false, false);

        // We are using easy mock to verify the calls (to deleteVirtualServer() ) instead of asserts.
        control.verify();
    }
}
