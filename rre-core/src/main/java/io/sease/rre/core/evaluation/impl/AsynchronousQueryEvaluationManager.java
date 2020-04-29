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
package io.sease.rre.core.evaluation.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.sease.rre.core.domain.Query;
import io.sease.rre.core.evaluation.EvaluationManager;
import io.sease.rre.core.template.QueryTemplateManager;
import io.sease.rre.core.version.VersionManager;
import io.sease.rre.persistence.PersistenceManager;
import io.sease.rre.search.api.SearchPlatform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous implementation of {@link EvaluationManager} that runs
 * evaluations and the queries inside them asynchronously. This splits the
 * given threadpool size into two pools - one for the evaluations, and
 * another for running the queries.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
public class AsynchronousQueryEvaluationManager extends BaseEvaluationManager implements EvaluationManager {

    private final static Logger LOGGER = LogManager.getLogger(AsynchronousQueryEvaluationManager.class);

    private final ThreadPoolExecutor executor;
    private final ExecutorService queryExecutor;

    /**
     * Construct an asynchronous {@link EvaluationManager} instance to run
     * evaluations using a threadpool of a given size. The threadpool will be
     * divided between the evaluation and query handlers.
     *
     * @param platform           the search engine in use.
     * @param templateManager    the template manager.
     * @param persistenceManager the persistence manager.
     * @param fields             the fields to return from each query.
     * @param versionManager     the version manager.
     * @param threadpoolSize     the maximum number of threads to use.
     */
    public AsynchronousQueryEvaluationManager(SearchPlatform platform, QueryTemplateManager templateManager, PersistenceManager persistenceManager, String[] fields, VersionManager versionManager, int threadpoolSize) {
        super(platform, templateManager, persistenceManager, fields, versionManager);
        int queryThreadpool = Math.min(threadpoolSize / 2, versionManager.getConfigurationVersions().size());
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadpoolSize - queryThreadpool);
        this.queryExecutor = Executors.newFixedThreadPool(queryThreadpool);
    }

    @Override
    public void evaluateQuery(Query query, String indexName, JsonNode queryNode, String defaultTemplate, int relevantDocCount) {
        startRunning();
        evaluateQueryAsync(query, indexName, queryNode, defaultTemplate, relevantDocCount)
                .thenAccept(this::completeQuery);
    }

    /**
     * Start an asynchronous evaluation where each versioned query is run
     * on its own thread.
     *
     * @param query            the query to be evaluated.
     * @param indexName        the base name of the index to query.
     * @param queryNode        the JSON node holding details of the query template.
     * @param defaultTemplate  the fallback query template.
     * @param relevantDocCount the number of relevant documents required.
     * @return a Future which, when complete, will contain the evaluated query.
     */
    private CompletableFuture<Query> evaluateQueryAsync(Query query, String indexName, JsonNode queryNode, String defaultTemplate, int relevantDocCount) {
        return CompletableFuture.supplyAsync(() -> {
            final CountDownLatch doneSignal = new CountDownLatch(getVersions().size());
            getVersions().forEach(version -> {
                final AtomicInteger rank = new AtomicInteger(1);
                // Queries are run in their own threadpool
                CompletableFuture.supplyAsync(() -> executeQuery(indexName, version, queryNode, defaultTemplate, relevantDocCount), queryExecutor)
                        .thenAccept(response -> {
                            query.setTotalHits(response.totalHits(), persistVersion(version));
                            response.hits().forEach(hit -> query.collect(hit, rank.getAndIncrement(), persistVersion(version)));
                            doneSignal.countDown();
                        });
            });
            try {
                doneSignal.await();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted waiting for queries to execute: {}", e.getMessage());
            }
            return query;
        }, executor);
    }

    @Override
    public boolean isRunning() {
        return executor.getCompletedTaskCount() < executor.getTaskCount();
    }

    @Override
    public int getQueriesRemaining() {
        return (int) (executor.getTaskCount() - executor.getCompletedTaskCount());
    }

    @Override
    public int getTotalQueries() {
        return (int) executor.getTaskCount();
    }

    @Override
    public void stop() {
        super.stopRunning();
        try {
            LOGGER.info("Waiting for asynchronous query evaluation threads to stop.");
            queryExecutor.shutdown();
            executor.shutdown();
            if (executor.awaitTermination(30, TimeUnit.SECONDS) && queryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                LOGGER.info("  ... all threads stopped within timeout period.");
            } else {
                LOGGER.info("  ... forcing executor shutdown.");
                if (!queryExecutor.isShutdown()) {
                    queryExecutor.shutdownNow();
                }
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted waiting for asynchronous query evaluation threads to stop.");
        }
    }
}
