/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sease.rre.search.api.impl;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for Solr Clients in use when connecting to external Solr instances.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
class SolrClientManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientManager.class);

    private final Map<String, SolrClient> indexClients = new HashMap<>();

    /**
     * Build a SolrClient instance, associating it with a specific target index
     * (or core).
     *
     * @param targetIndexName the name of the index/core this client should be
     *                        used with.
     * @param settings        the {@link io.sease.rre.search.api.impl.ExternalApacheSolr.SolrSettings}
     *                        containing the client connection details.
     */
    void buildSolrClient(String targetIndexName, ExternalApacheSolr.SolrSettings settings) {
        final SolrClient client;

        final HttpClient httpClient = HttpClients.createDefault();
        if (settings.getConnectionTimeout() != null) {
            HttpClientUtil.setConnectionTimeout(httpClient, settings.getConnectionTimeout());
        }
        if (settings.getSocketTimeout() != null) {
            HttpClientUtil.setSoTimeout(httpClient, settings.getSocketTimeout());
        }

        if (settings.hasZookeeperSettings()) {
            client = new CloudSolrClient.Builder()
                    .withHttpClient(httpClient)
                    .withZkHost(settings.getZkHosts())
                    .withZkChroot(settings.getZkChroot())
                    .build();
        } else if (settings.getBaseUrls().size() > 1) {
            client = new CloudSolrClient.Builder()
                    .withHttpClient(httpClient)
                    .withSolrUrl(settings.getBaseUrls())
                    .build();
        } else {
            client = new HttpSolrClient.Builder(settings.getBaseUrls().get(0))
                    .withHttpClient(httpClient)
                    .build();
        }

        indexClients.put(targetIndexName, client);
    }

    /**
     * Get the SolrClient for a specific target index.
     *
     * @param targetIndexName the name of the index/core whose client is
     *                        required.
     * @return the client, or {@code null} if no client has been set for the
     * target index.
     */
    SolrClient getSolrClient(String targetIndexName) {
        return indexClients.get(targetIndexName);
    }

    /**
     * Ensure that all of the index clients are closed.
     */
    public void close() {
        indexClients.values().forEach(c -> {
            try {
                c.close();
            } catch (IOException e) {
                LOGGER.error("Caught IOException closing client: {}", e.getMessage());
            }
        });
    }
}
