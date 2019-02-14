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
package io.sease.rre.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.sease.rre.core.domain.metrics.HitsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A search response whcih gradually collects a set of search hits.
 * The prefix "Mutable" is because the RRE core classes contain a similar class which is supposed to be Immutable.
 *
 * @author agazzarini
 * @since 1.0
 */
public class MutableQueryOrSearchResponse implements HitsCollector {
    private long totalHits;
    private List<Map<String, Object>> hits = new ArrayList<>();

    /**
     * Returns the total hits number associated with this response.
     *
     * @return the total hits number associated with this response.
     */
    @JsonProperty("total-hits")
    public long totalHits() {
        return totalHits;
    }

    /**
     * Returns the current hits window.
     *
     * @return the current hits window.
     */
    @JsonProperty("hits")
    public List<Map<String, Object>> hits() {
        return hits;
    }

    @Override
    public void collect(final Map<String, Object> hit, final int rank, final String version) {
        hits.add(hit);
    }

    @Override
    public void setTotalHits(final long totalHits, final String version) {
        this.totalHits = totalHits;
    }
}
