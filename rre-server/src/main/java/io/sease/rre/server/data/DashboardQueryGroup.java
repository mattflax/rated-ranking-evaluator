package io.sease.rre.server.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A QueryGroup object, as passed from the dashboard. Includes the topic
 * and corpus detail to allow for correct filtering.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardQueryGroup {

    private final String corpus;
    private final String topic;
    private final String queryGroup;

    public DashboardQueryGroup(@JsonProperty("queryGroup") String queryGroup,
                               @JsonProperty("topic") String topic,
                               @JsonProperty("corpus") String corpus) {
        this.corpus = corpus;
        this.topic = topic;
        this.queryGroup = queryGroup;
    }

    public String getCorpus() {
        return corpus;
    }

    public String getTopic() {
        return topic;
    }

    public String getQueryGroup() {
        return queryGroup;
    }
}
