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
package io.sease.rre.core.domain.metrics.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.core.domain.metrics.ValueFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static io.sease.rre.Func.gainOrRatingNode;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

/**
 * NDCG@10 metric.
 *
 * @author agazzarini
 * @since 1.0
 */
public class NDCGAtTen extends Metric {
    private final static BigDecimal TWO = new BigDecimal(2);

    /**
     * Builds a new NDCGAtTen metric.
     */
    public NDCGAtTen() {
        super("NDCG@10");
    }

    @Override
    public ValueFactory createValueFactory(final String version) {
        return new ValueFactory(this, version) {
            private BigDecimal dcg = BigDecimal.ZERO;

            @Override
            public void collect(final Map<String, Object> hit, final int rank, final String version) {
                if (rank > 10) return;
                judgment(id(hit))
                        .ifPresent(judgment -> {
                            final BigDecimal value = gainOrRatingNode(judgment).map(JsonNode::decimalValue).orElse(TWO);
                            switch (rank) {
                                case 1:
                                    dcg = value;
                                    break;
                                default:
                                    dcg = dcg.add(new BigDecimal(value.doubleValue() / (Math.log(rank + 1) / Math.log(2))));
                            }
                        });
            }

            @Override
            public BigDecimal value() {
                if (totalHits == 0) {
                    return relevantDocuments.size() == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
                }

                final BigDecimal idealDcg = idealDcg(relevantDocuments);
                if (dcg.equals(BigDecimal.ZERO) && idealDcg.equals(BigDecimal.ZERO)) {
                    return BigDecimal.ZERO;
                }

                return dcg.divide(idealDcg, 2, RoundingMode.FLOOR);
            }
        };
    }

    private BigDecimal idealDcg(final JsonNode relevantDocuments) {
        final int windowSize = Math.min(relevantDocuments.size(), 10);
        final int[] gains = new int[windowSize];

        final Map<Integer, List<JsonNode>> groups =
                StreamSupport.stream(relevantDocuments.spliterator(), false)
                                .collect(groupingBy(doc -> gainOrRatingNode(doc).map(JsonNode::intValue).orElse(2)));

        final int veryVeryRelevantDocsCount = groups.getOrDefault(3, emptyList()).size();
        final int howManyVeryVeryRelevantDocs = Math.min(veryVeryRelevantDocsCount, windowSize);

        final int veryRelevantDocsCount = groups.getOrDefault(2, emptyList()).size();
        final int howManyVeryRelevantDocs = Math.min(veryRelevantDocsCount, windowSize - howManyVeryVeryRelevantDocs);

        final int marginallyRelevantDocsCount = groups.getOrDefault(1, emptyList()).size();
        final int howManyMarginallyRelevantDocs = Math.max(Math.min(marginallyRelevantDocsCount - howManyVeryRelevantDocs, 0), 0);

        Arrays.fill(gains, 0, howManyVeryVeryRelevantDocs, 3);
        if (howManyVeryVeryRelevantDocs < windowSize) {
            Arrays.fill(gains, howManyVeryVeryRelevantDocs, howManyVeryVeryRelevantDocs + howManyVeryRelevantDocs, 2);
        }

        if (howManyVeryVeryRelevantDocs + howManyVeryRelevantDocs < windowSize) {
            Arrays.fill(gains, howManyVeryVeryRelevantDocs + howManyVeryRelevantDocs, gains.length, 1);
        }

        BigDecimal result = gains.length > 0 ? new BigDecimal(gains[0]) : BigDecimal.ZERO;
        for (int i = 1; i < gains.length; i++) {
            result = result.add(new BigDecimal(gains[i] / (Math.log(i + 2) / Math.log(2))));
        }

        return result;
    }
}
