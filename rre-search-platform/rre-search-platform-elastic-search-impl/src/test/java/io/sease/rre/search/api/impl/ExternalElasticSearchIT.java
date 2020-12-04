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

import io.sease.rre.search.api.SearchPlatform;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for the External Elasticsearch implementation.
 * <p>
 * These won't be run as part of the main test phase, only as part of the
 * "integration" profile.
 * <p>
 * These use the TestContainers framework, which spins up Docker containers
 * to allow testing against a Solr instance.
 *
 * @author Matt Pearce (matt@elysiansoftware.co.uk)
 */
public class ExternalElasticSearchIT {

	private static String ELASTICSEARCH_CONTAINER_BASE = "docker.elastic.co/elasticsearch/elasticsearch";
	private static String DEFAULT_ELASTICSEARCH_VERSION = "7.5.0";

	private static final String INDEX_NAME = "test";
	private static final String INDEX_VERSION = "v1.0";

	private static final DockerImageName DOCKER_IMAGE = DockerImageName.parse(ELASTICSEARCH_CONTAINER_BASE + ":" + System.getProperty("elasticsearch.version", DEFAULT_ELASTICSEARCH_VERSION));

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SearchPlatform platform;

	@Before
	public void setupPlatform() throws Exception {
		platform = new ExternalElasticsearch();
	}

	@Test
	public void checkCollection_returnsFalseWhenNotLoaded() throws Exception {
		final ElasticsearchContainer es = new ElasticsearchContainer(DOCKER_IMAGE);
		es.start();

		final File settingsFile = tempFolder.newFile("ccrf_settings.json");
		FileWriter fw = new FileWriter(settingsFile);
		fw.write("{ \"hostUrls\": [ \"" + es.getHttpHostAddress() + "\" ]}");
		fw.close();

		platform.load(null, settingsFile, INDEX_NAME, INDEX_VERSION);

		assertFalse(platform.checkCollection(INDEX_NAME, INDEX_VERSION));
		es.close();
	}

	@Test
	public void checkCollection_returnsTrueWhenAvailable() throws Exception {
		final ElasticsearchContainer es = new ElasticsearchContainer(DOCKER_IMAGE);
		es.start();

		// Create an index
		final RestHighLevelClient hlClient = new RestHighLevelClient(RestClient.builder(HttpHost.create(es.getHttpHostAddress())));
		hlClient.indices().create(new CreateIndexRequest(INDEX_NAME), RequestOptions.DEFAULT);

		final File settingsFile = tempFolder.newFile("ccrt_settings.json");
		FileWriter fw = new FileWriter(settingsFile);
		fw.write("{ \"hostUrls\": [ \"" + es.getHttpHostAddress() + "\" ]}");
		fw.close();

		platform.load(null, settingsFile, INDEX_NAME, INDEX_VERSION);

		assertTrue(platform.checkCollection(INDEX_NAME, INDEX_VERSION));
		hlClient.close();
		es.close();
	}

}
