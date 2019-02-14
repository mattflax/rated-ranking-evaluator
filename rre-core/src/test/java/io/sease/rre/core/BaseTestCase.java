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
package io.sease.rre.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sease.rre.Field;
import io.sease.rre.core.domain.metrics.Metric;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.sease.rre.core.TestData.A_VERSION;
import static io.sease.rre.core.TestData.DOCUMENTS_SETS;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;

/**
 * Supertype layers for all unit test cases.
 *
 * @author agazzarini
 * @since 1.0
 */
public abstract class BaseTestCase {
    protected final ObjectMapper mapper = new ObjectMapper();

    protected Metric cut;
    protected AtomicInteger counter;

    /**
     * Returns the class under test.
     *
     * @return the class under test.
     */
    protected Metric cut() {
        return cut;
    }

    /**
     * Setup fixture for this test case.
     */
    public abstract void setUp();

    /**
     * If there are no relevant results and we have an empty resultset, then (symbolic) P is 1.
     */
    @Test
    public void noRelevantDocumentsAndNoSearchResults() {
        cut().setRelevantDocuments(mapper.createObjectNode());
        cut().setTotalHits(0, A_VERSION);

        assertEquals(BigDecimal.ONE.doubleValue(), cut().valueFactory(A_VERSION).value().doubleValue(), 0);
    }

    /**
     * If there are no relevant results and we haven't an empty resultset, then the value should be 0.
     */
    @Test
    public void noRelevantDocumentsWithSearchResults() {
        stream(DOCUMENTS_SETS).forEach(set -> {
            cut().setRelevantDocuments(mapper.createObjectNode());
            cut().setTotalHits(set.length, A_VERSION);

            stream(set)
                    .map(this::searchHit)
                    .forEach(hit -> cut().collect(hit, counter.incrementAndGet(), A_VERSION));

            assertEquals(
                    BigDecimal.ZERO.doubleValue(),
                    cut().valueFactory(A_VERSION).value().doubleValue(), 0);
            setUp();
        });
    }

    /**
     * Creates a JSON object node representation of a document judgment.
     *
     * @param gain the gain that will be associated with the judgment.
     * @return a JSON object node representation of a document judgment.
     */
    protected JsonNode createJudgmentNode(final int gain) {
        final ObjectNode judgment = mapper.createObjectNode();
        judgment.put(Field.GAIN, gain);
        return judgment;
    }

    /**
     * Generates a search hit used for testing purposes.
     * Note that, for evaluating, we just need the document identifier.
     * That's the reason why the generated document has no other fields.
     *
     * @param id the document identifier.
     * @return a search hit used for testing purposes.
     */
    protected Map<String, Object> searchHit(final String id) {
        final Map<String, Object> doc = new HashMap<>();
        doc.put("id", id);
        return doc;
    }

    /**
     * If all results in the window are relevant, then the metric value is 1.
     */
    public void maximum() {
        stream(DOCUMENTS_SETS).forEach(set -> {
            final ObjectNode judgements = mapper.createObjectNode();
            stream(set).forEach(docid -> judgements.set(docid, createJudgmentNode(3)));

            cut().setRelevantDocuments(judgements);
            cut().setTotalHits(set.length, A_VERSION);

            stream(set)
                    .map(this::searchHit)
                    .forEach(hit -> cut().collect(hit, counter.incrementAndGet(), A_VERSION));

            assertEquals(
                    "Fail to assert dataset with " + set.length + " items.",
                    BigDecimal.ONE.doubleValue(),
                    cut().valueFactory(A_VERSION).value().doubleValue(),
                    0);
            setUp();
        });
    }
}