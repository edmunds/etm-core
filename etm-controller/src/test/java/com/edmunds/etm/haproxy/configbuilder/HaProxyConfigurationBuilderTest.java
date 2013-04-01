package com.edmunds.etm.haproxy.configbuilder;

import com.edmunds.etm.common.api.FixedUrlToken;
import com.edmunds.etm.common.api.RegexUrlToken;
import com.edmunds.etm.loadbalancer.api.PoolMember;
import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.management.api.HostAddress;
import com.edmunds.etm.management.api.HttpMonitor;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import com.edmunds.etm.runtime.api.Application;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Test
public class HaProxyConfigurationBuilderTest {

    @Test
    public void testApacheConfigGeneration() throws Exception {

        final UrlTokenDictionary urlTokenResolver = new UrlTokenDictionary();

        urlTokenResolver.add(new RegexUrlToken("model", "[^/]*"));
        urlTokenResolver.add(new RegexUrlToken("year", "(19|20)\\d{2}"));
        urlTokenResolver.add(new FixedUrlToken("make",
                "acura", "am-general", "amgeneral", "aston-martin", "astonmartin", "audi", "bentley", "bmw", "bugatti",
                "buick", "cadillac", "chevrolet", "chrysler", "daewoo", "dodge", "dummy", "eagle", "ferrari", "fiat",
                "fisker", "ford", "geo", "gmc", "honda", "hummer", "hyundai", "infiniti", "isuzu", "jaguar", "jeep",
                "kia", "lamborghini", "land-rover", "landrover", "lexus", "lincoln", "lotus", "mahindra", "maserati",
                "maybach", "mazda", "mclaren", "mercedes-benz", "mercedesbenz", "mercury", "mini", "mitsubishi",
                "nissan", "oldsmobile", "panoz", "plymouth", "pontiac", "porsche", "ram", "rolls-royce", "rollsroyce",
                "saab", "saturn", "scion", "smart", "spyker", "srt", "subaru", "suzuki", "tesla", "toyota",
                "volkswagen", "volvo", "alfa-romeo"));

        final HaProxyConfigurationBuilder configBuilder = new HaProxyConfigurationBuilder();
        configBuilder.setUrlTokenResolver(urlTokenResolver);

        final List<String> lines = loadUrlLines();
        final List<UrlRule> urlRules = loadUrlRules(urlTokenResolver, lines);
        final Set<MavenModule> mavenModules = getMavenModules(urlRules);
        final List<Application> applications = buildApplications(mavenModules);

        final byte[] result = configBuilder.build(applications, urlRules);

        // Load the expected value
        final byte[] expectedConfig = loadHaProxyConfig();
        assertNotNull(expectedConfig, "Unable to load sample HA Proxy config");

        assertNotNull(result);
        assertEquals(result, expectedConfig);
    }

    private List<Application> buildApplications(Set<MavenModule> mavenModules) {
        final HttpMonitor httpMonitor = new HttpMonitor("/support-internal/index.jsp", "Admin Home");
        final HostAddress vipHost = new HostAddress("localhost", 8000);

        final List<Application> applications = Lists.newArrayList();

        int appCount = 1;
        for (MavenModule mavenModule : mavenModules) {
            final VirtualServer virtualServer = new VirtualServer(mavenModule.getArtifactId(), vipHost, buildPoolMembers(appCount++));
            applications.add(new Application(mavenModule, new ArrayList<String>(), httpMonitor, virtualServer));
        }
        return applications;
    }

    private Set<PoolMember> buildPoolMembers(final int appCount) {
        final Set<PoolMember> poolMembers = Sets.newHashSet();

        for (int i = 1; i < 4; i++) {
            final String host = String.format("10.2.%d.%d", appCount, i);
            poolMembers.add(new PoolMember(new HostAddress(host, 9000)));
        }

        return poolMembers;
    }

    private List<String> loadUrlLines() throws IOException {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/url-rules.txt");

            return IOUtils.readLines(stream, "UTF8");
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private List<UrlRule> loadUrlRules(UrlTokenResolver urlTokenResolver, List<String> lines) {
        final List<UrlRule> urlRules = Lists.newArrayList();

        for (String line : lines) {
            final String[] split = line.split("\t");
            final String rule = split[0];
            final String gav = split[1];
            final String vipAddress = split[2];

            final String[] gavSplit = gav.split(":");
            final MavenModule mavenModule = new MavenModule(gavSplit[0], gavSplit[1], gavSplit[2]);

            urlRules.add(new UrlRule(urlTokenResolver, mavenModule, vipAddress, rule));
        }

        return urlRules;
    }

    private Set<MavenModule> getMavenModules(List<UrlRule> urlRules) {
        final Set<MavenModule> modules = Sets.newTreeSet();

        for (final UrlRule urlRule : urlRules) {
            modules.add(urlRule.getMavenModule());
        }

        return modules;
    }

    private byte[] loadHaProxyConfig() throws IOException {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/haproxy.conf");

            return IOUtils.toByteArray(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
