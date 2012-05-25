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
package com.edmunds.etm.rules.impl;

import com.edmunds.etm.common.api.ControllerPaths;
import com.edmunds.etm.common.api.UrlToken;
import com.edmunds.etm.common.impl.ObjectSerializer;
import com.edmunds.etm.common.impl.UrlTokenRepository;
import com.edmunds.etm.common.thrift.UrlTokenDto;
import com.edmunds.etm.rules.api.UrlTokenChangeListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnection;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionListener;
import com.edmunds.zookeeper.connection.ZooKeeperConnectionState;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeConsistentCallback;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeNode;
import com.edmunds.zookeeper.treewatcher.ZooKeeperTreeWatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repository for the values of fixed UrlToken objects.
 *
 * @author Ryan Holmes
 */
@Component
public class UrlTokenMonitor implements ZooKeeperConnectionListener, ServletContextAware {

    public static final String DEFAULT_TOKENS_XML_PATH = "/WEB-INF/default-url-tokens.xml";

    private static final Logger logger = Logger.getLogger(UrlTokenMonitor.class);

    private final ZooKeeperTreeWatcher tokenNodeWatcher;
    private final ObjectSerializer objectSerializer;
    private final UrlTokenDictionary tokenDictionary;
    private final UrlTokenRepository tokenRepository;

    private ServletContext servletContext;
    private Set<UrlTokenChangeListener> tokenChangeListeners;
    private Set<UrlTokenDto> previousTokenDtos;
    private boolean tokensInitialized;

    @Autowired
    public UrlTokenMonitor(ZooKeeperConnection connection,
                           ControllerPaths controllerPaths,
                           ObjectSerializer objectSerializer,
                           UrlTokenDictionary tokenDictionary,
                           UrlTokenRepository tokenRepository) {
        ZooKeeperTreeConsistentCallback cb = new ZooKeeperTreeConsistentCallback() {
            @Override
            public void treeConsistent(ZooKeeperTreeNode oldRoot, ZooKeeperTreeNode newRoot) {
                onTokenTreeChanged(newRoot);
            }
        };
        this.objectSerializer = objectSerializer;
        this.tokenDictionary = tokenDictionary;
        this.tokenRepository = tokenRepository;

        String nodePath = controllerPaths.getUrlTokens();
        this.tokenNodeWatcher = new ZooKeeperTreeWatcher(connection, 0, nodePath, cb);

        this.tokenChangeListeners = Sets.newHashSet();
        this.previousTokenDtos = Sets.newHashSet();
        this.tokensInitialized = false;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void onConnectionStateChanged(ZooKeeperConnectionState state) {
        if (state == ZooKeeperConnectionState.INITIALIZED) {
            tokenNodeWatcher.initialize();
        }
    }

    /**
     * Adds a listener to receive notification of changes to URL tokens.
     *
     * @param listener url token listener
     */
    public void addListener(UrlTokenChangeListener listener) {
        tokenChangeListeners.add(listener);
    }

    private void onTokenTreeChanged(ZooKeeperTreeNode tokenNode) {

        if (tokenNode != null) {
            Collection<ZooKeeperTreeNode> childNodes = tokenNode.getChildren().values();
            Set<UrlTokenDto> tokenDtos = Sets.newHashSetWithExpectedSize(childNodes.size());
            for (ZooKeeperTreeNode node : childNodes) {
                try {
                    UrlTokenDto dto = objectSerializer.readValue(node.getData(), UrlTokenDto.class);
                    tokenDtos.add(dto);
                } catch (IOException e) {
                    logger.error(String.format("Unable to deserialize UrlToken node: %s", node.getPath()));
                }
            }
            processTokenDtos(tokenDtos);
        }
    }

    private void processTokenDtos(Set<UrlTokenDto> dtos) {

        // load default tokens at startup if none are defined
        if (!tokensInitialized && dtos.isEmpty()) {
            loadDefaultTokens();
            return;
        }

        // don't proceed if tokens are unchanged
        if (tokensInitialized && dtos.equals(previousTokenDtos)) {
            return;
        }
        previousTokenDtos = dtos;

        // update the token dictionary and notify listeners
        List<UrlToken> tokens = Lists.newArrayListWithCapacity(dtos.size());
        for (UrlTokenDto dto : dtos) {
            UrlToken token = UrlToken.readDto(dto);
            tokens.add(token);
        }
        tokenDictionary.clear();
        tokenDictionary.addAll(tokens);

        processChangeEvent();
        tokensInitialized = true;
    }

    private void processChangeEvent() {
        logger.info("URL tokens changed");

        for (UrlTokenChangeListener listener : tokenChangeListeners) {
            listener.onUrlTokensChanged(tokenDictionary);
        }
    }

    private void loadDefaultTokens() {
        logger.debug("Loading default URL tokens");
        String contextPath = servletContext.getRealPath("/");
        File file = new File(contextPath + DEFAULT_TOKENS_XML_PATH);
        try {
            tokenRepository.loadTokensFromFile(file, true);
        } catch (IOException e) {
            logger.error("Unable to load default URL tokens", e);
        }
    }
}
