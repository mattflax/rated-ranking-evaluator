package io.sease.rre.server.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Topic object, used when passing filter queries from the dashboard.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardTopic {

    private final String topicName;
    private final String corpus;

    public DashboardTopic(@JsonProperty("topicName") String topicName, @JsonProperty("corpus") String corpus) {
        this.topicName = topicName;
        this.corpus = corpus;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getCorpus() {
        return corpus;
    }
}
