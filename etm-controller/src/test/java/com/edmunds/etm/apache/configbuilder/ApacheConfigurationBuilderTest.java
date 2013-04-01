package com.edmunds.etm.apache.configbuilder;

import com.edmunds.etm.common.api.FixedUrlToken;
import com.edmunds.etm.common.api.RegexUrlToken;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.rules.impl.UrlTokenDictionary;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Test
public class ApacheConfigurationBuilderTest {

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
                "volkswagen", "volvo"));

        final ApacheConfigurationBuilder configBuilder = new ApacheConfigurationBuilder();
        configBuilder.setUrlTokenResolver(urlTokenResolver);

        final List<String> lines = loadUrlLines();
        final List<UrlRule> urlRules = loadUrlRules(urlTokenResolver, lines);
        final byte[] result = configBuilder.build(null, urlRules);

        // Load the expected value
        final byte[] expectedConfig = loadApacheConfig();
        assertNotNull(expectedConfig, "Unable to load sample Apache config");

        assertNotNull(result);
        assertEquals(result, expectedConfig);
    }

    private List<UrlRule> loadUrlRules(UrlTokenResolver urlTokenResolver, List<String> lines) {
        final MavenModule mavenModule = new MavenModule("com.edmunds", "test-artifact", "1.0.0");

        final List<UrlRule> urlRules = Lists.newArrayList();

        for (String line : lines) {
            final String[] split = line.split("\t");
            final String rule = split[0];
            final String vipAddress = split[2];
            urlRules.add(new UrlRule(urlTokenResolver, mavenModule, vipAddress, rule));
        }

        return urlRules;
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

    private byte[] loadApacheConfig() throws IOException {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/apache.conf");

            return IOUtils.toByteArray(stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
