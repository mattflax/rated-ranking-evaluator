package io.sease.rre.server.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * Filter data, passed from the dashboard to be used for filtering an
 * evaluation.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardFilterData {

    private final Collection<String> corpora;
    private final Collection<DashboardTopic> topics;
    private final Collection<DashboardQueryGroup> queryGroups;
    private final Collection<String> metrics;
    private final Collection<String> versions;

    public DashboardFilterData(@JsonProperty("corpora") Collection<String> corpora,
                               @JsonProperty("topics") Collection<DashboardTopic> topics,
                               @JsonProperty("queryGroups") Collection<DashboardQueryGroup> queryGroups,
                               @JsonProperty("metrics") Collection<String> metrics,
                               @JsonProperty("versions") Collection<String> versions) {
        this.corpora = corpora;
        this.topics = topics;
        this.queryGroups = queryGroups;
        this.metrics = metrics;
        this.versions = versions;
    }

    public Collection<String> getCorpora() {
        return corpora;
    }

    public Collection<DashboardTopic> getTopics() {
        return topics;
    }

    public Collection<DashboardQueryGroup> getQueryGroups() {
        return queryGroups;
    }

    public Collection<String> getMetrics() {
        return metrics;
    }

    public Collection<String> getVersions() {
        return versions;
    }
}
