package io.sease.rre.server.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * An individual query result, representing a single versioned query with
 * metrics and metadata.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchQueryResult {

    private final String id;
    private final String corpora;
    private final String topic;
    private final String queryGroup;
    private final String queryText;
    private final String version;
    private final long totalHits;
    private final List<QueryMetric> metrics;

    public ElasticsearchQueryResult(@JsonProperty("id") String id,
                                    @JsonProperty("corpora") String corpora,
                                    @JsonProperty("topic") String topic,
                                    @JsonProperty("queryGroup") String queryGroup,
                                    @JsonProperty("queryText") String queryText,
                                    @JsonProperty("version") String version,
                                    @JsonProperty("totalHits") long totalHits,
                                    @JsonProperty("metrics") List<QueryMetric> metrics) {
        this.id = id;
        this.corpora = corpora;
        this.topic = topic;
        this.queryGroup = queryGroup;
        this.queryText = queryText;
        this.version = version;
        this.totalHits = totalHits;
        this.metrics = metrics;
    }

    public String getId() {
        return id;
    }

    public String getCorpora() {
        return corpora;
    }

    public String getTopic() {
        return topic;
    }

    public String getQueryGroup() {
        return queryGroup;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getVersion() {
        return version;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public List<QueryMetric> getMetrics() {
        return metrics;
    }


    /**
     * A query metric, including both its name and the sanitised name, and
     * the metric value.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryMetric {
        private final String name;
        private final String sanitisedName;
        private final double value;

        public QueryMetric(@JsonProperty("name") String name,
                           @JsonProperty("sanitisedName") String sanitisedName,
                           @JsonProperty("value") double value) {
            this.name = name;
            this.sanitisedName = sanitisedName;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getSanitisedName() {
            return sanitisedName;
        }

        public double getValue() {
            return value;
        }
    }
}
