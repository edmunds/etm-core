package com.edmunds.etm.haproxy.configbuilder;

import com.edmunds.etm.loadbalancer.api.VirtualServer;
import com.edmunds.etm.management.api.MavenModule;
import com.edmunds.etm.rules.api.UrlRule;
import com.edmunds.etm.rules.api.UrlTokenResolver;
import com.edmunds.etm.rules.api.WebServerConfigurationBuilder;
import com.edmunds.etm.runtime.api.Application;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Generates the configuration for HA Proxy.
 */
@Component
public class HaProxyConfigurationBuilder implements WebServerConfigurationBuilder {

    private final Configuration freemarkerConfiguration;

    private UrlTokenResolver urlTokenResolver;

    private byte[] activeRuleSetData;
    private String activeRuleSetDigest;

    public HaProxyConfigurationBuilder() {
        this.freemarkerConfiguration = new Configuration();
        this.freemarkerConfiguration.setClassForTemplateLoading(getClass(), "");
        this.activeRuleSetData = new byte[0];
        this.activeRuleSetDigest = "";
    }

    /**
     * Sets the url token resolver.
     *
     * @param urlTokenResolver the url token resolver
     */
    @Autowired
    public void setUrlTokenResolver(UrlTokenResolver urlTokenResolver) {
        this.urlTokenResolver = urlTokenResolver;
    }

    @Override
    public String getZooKeeperNodeName() {
        return "haproxy";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] build(Collection<Application> applications, Collection<UrlRule> urlRules) {
        final List<RuleWrapper> ruleWrappers = Lists.newArrayList();
        for (UrlRule urlRule : urlRules) {
            ruleWrappers.add(new RuleWrapper(urlRule));
        }
        final String defaultBackend = getDefaultBackend(urlRules);

        final Map<String, Object> model = Maps.newHashMap();

        model.put("applications", applications);
        model.put("rules", ruleWrappers);
        model.put("defaultBackend", defaultBackend);

        final byte[] ruleSet = processTemplate(model);
        updateActiveRuleSet(ruleSet);
        return ruleSet;
    }

    private byte[] processTemplate(Map<String, Object> root) {
        ByteArrayOutputStream baos = null;
        OutputStreamWriter out = null;

        try {
            baos = new ByteArrayOutputStream();
            out = new OutputStreamWriter(baos, "UTF8");
            freemarkerConfiguration.getTemplate("haproxy.ftl").process(root, out);
            out.close();
        } catch (TemplateException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(baos);
        }

        return baos.toByteArray();
    }

    private String getDefaultBackend(final Collection<UrlRule> rules) {
        for (final UrlRule rule : rules) {
            if ("/**".equals(rule.getRule())) {
                return rule.getMavenModule().getArtifactId();
            }
        }

        return null;
    }

    @Override
    public synchronized byte[] getActiveRuleSetData() {
        return activeRuleSetData == null ? null : activeRuleSetData.clone();
    }

    @Override
    public synchronized String getActiveRuleSetDigest() {
        return activeRuleSetDigest;
    }

    private synchronized void updateActiveRuleSet(byte[] data) {
        activeRuleSetData = data;
        activeRuleSetDigest = data != null ? DigestUtils.md5Hex(data) : "";
    }

    /**
     * Wrapper used to expose VIP's to the freemarker template.
     */
    public static class VipWrapper {
        private final MavenModule mavenModule;
        private final VirtualServer virtualServer;

        public VipWrapper(MavenModule mavenModule, VirtualServer virtualServer) {
            this.mavenModule = mavenModule;
            this.virtualServer = virtualServer;
        }

        public MavenModule getMavenModule() {
            return mavenModule;
        }

        public String getArtifactId() {
            return mavenModule.getArtifactId();
        }

        public VirtualServer getVirtualServer() {
            return virtualServer;
        }
    }

    /**
     * Wrapper used to expose rules to the freemarker template.
     */
    public class RuleWrapper {
        private final UrlRule urlRule;
        private final String regEx;

        RuleWrapper(UrlRule urlRule) {
            this.urlRule = urlRule;
            this.regEx = urlRule.toRegEx(urlTokenResolver);
        }

        public UrlRule getUrlRule() {
            return urlRule;
        }

        public String getRegEx() {
            return regEx;
        }

        public String getArtifactId() {
            return urlRule.getMavenModule().getArtifactId();
        }
    }
}
