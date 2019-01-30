package io.sease.rre.server.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sease.rre.core.domain.*;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.server.data.DashboardQueryGroup;
import io.sease.rre.server.data.DashboardTopic;
import io.sease.rre.server.domain.EvaluationMetadata;
import io.sease.rre.server.domain.StaticMetric;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;

/**
 * Implementation of the evaluation manager service which will extract a
 * complete Evaluation object from the request data.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@Service
@Profile({"http", "default"})
public class HttpEvaluationHandlerService implements EvaluationHandlerService {

    private final ObjectMapper mapper = new ObjectMapper();

    private Evaluation evaluation = new Evaluation();
    private EvaluationMetadata metadata = new EvaluationMetadata(Collections.emptyList(), Collections.emptyList());

    @Override
    public void processEvaluationRequest(final JsonNode requestData) throws EvaluationHandlerException {
        evaluation = make(requestData);
    }

    @Override
    public Evaluation getEvaluation() {
        return evaluation;
    }

    @Override
    public EvaluationMetadata getEvaluationMetadata() {
        return metadata;
    }

    void setEvaluation(Evaluation eval) {
        this.evaluation = eval;
        this.metadata = extractEvaluationMetadata(eval);
    }

    ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Creates an evaluation object from the input JSON data.
     *
     * @param data the JSON payload.
     * @return a session evaluation instance.
     */
    protected Evaluation make(final JsonNode data) {
        final Evaluation evaluation = new Evaluation();
        evaluation.setName(data.get("name").asText());

        metrics(data, evaluation);

        data.get("corpora").iterator().forEachRemaining(corpusNode -> {
            final String cname = corpusNode.get("name").asText();
            final Corpus corpus = evaluation.findOrCreate(cname, Corpus::new);

            metrics(corpusNode, corpus);

            corpusNode.get("topics").iterator().forEachRemaining(topicNode -> {
                final String tname = topicNode.get("name").asText();
                final Topic topic = corpus.findOrCreate(tname, Topic::new);
                metrics(topicNode, topic);

                topicNode.get("query-groups").iterator().forEachRemaining(groupNode -> {
                    final String gname = groupNode.get("name").asText();
                    final QueryGroup group = topic.findOrCreate(gname, QueryGroup::new);
                    metrics(groupNode, group);

                    groupNode.get("query-evaluations").iterator().forEachRemaining(queryNode -> {
                        final String qename = queryNode.get("query").asText();
                        final Query q = group.findOrCreate(qename, Query::new);
                        metrics(queryNode, q);


                        queryNode.get("results").fields().forEachRemaining(resultsEntry -> {
                            final MutableQueryOrSearchResponse versionedResponse =
                                    q.getResults().computeIfAbsent(
                                            resultsEntry.getKey(),
                                            version -> new MutableQueryOrSearchResponse());

                            JsonNode content = resultsEntry.getValue();
                            versionedResponse.setTotalHits(content.get("total-hits").asLong(), null);

                            stream(content.get("hits").spliterator(), false)
                                    .map(hit -> mapper.convertValue(hit, Map.class))
                                    .forEach(hit -> versionedResponse.collect(hit, -1, null));
                        });
                    });
                });
            });
        });

        return evaluation;
    }

    private void metrics(final JsonNode data, final DomainMember<?> parent) {
        data.get("metrics").fields().forEachRemaining(entry -> {
            final StaticMetric metric = new StaticMetric(entry.getKey());

            entry.getValue().get("versions").fields().forEachRemaining(vEntry ->
                metric.collect(vEntry.getKey(),
                        new BigDecimal(vEntry.getValue().get("value")
                                .asDouble())
                                .setScale(4, RoundingMode.CEILING)));
            parent.getMetrics().put(metric.getName(), metric);
        });
    }

    /**
     * Extract the evaluation metadata from an evaluation.
     *
     * @param evaluation the evaluation data.
     * @return the evaluation metadata.
     */
    public static EvaluationMetadata extractEvaluationMetadata(final Evaluation evaluation) {
        final List<String> metrics = new ArrayList<>(
                evaluation.getChildren()
                        .iterator().next()
                        .getMetrics().keySet());

        final List<String> versions = new ArrayList<>(
                evaluation.getChildren()
                        .iterator().next()
                        .getMetrics().values().iterator().next().getVersions().keySet());

        return new EvaluationMetadata(versions, metrics);
    }

    @Override
    public List<String> getMetrics() {
        return new ArrayList<>(evaluation.getMetrics().keySet());
    }

    @Override
    public List<String> getVersions() {
        final List<String> versions;

        if (evaluation.getMetrics() == null || evaluation.getMetrics().isEmpty()) {
            versions = Collections.emptyList();
        } else {
            // Simply get the first metric from the metric map...
            Metric m = evaluation.getMetrics().values().iterator().next();
            // ... and extract the versions
            versions = new ArrayList<>(m.getVersions().keySet());
        }

        return versions;
    }

    @Override
    public List<String> getCorpusNames() {
        final List<String> corpusNames;

        if (evaluation.getChildren() == null) {
            corpusNames = Collections.emptyList();
        } else {
            corpusNames = evaluation.getChildren().stream()
                    .map(Corpus::getName)
                    .collect(Collectors.toList());
        }

        return corpusNames;
    }

    @Override
    public List<String> getTopicNames(String corpus) {
        final List<String> topicNames;

        if (evaluation.getChildren() == null || corpus == null) {
            topicNames = Collections.emptyList();
        } else {
            topicNames = evaluation.getChildren().stream()
                    .filter(c -> c.getName().equals(corpus))
                    .flatMap(c -> c.getChildren().stream())
                    .map(Topic::getName)
                    .collect(Collectors.toList());
        }

        return topicNames;
    }

    @Override
    public List<String> getQueryGroupNames(String corpus, String topic) {
        final List<String> queryGroupNames;

        if (corpus == null || topic == null) {
            queryGroupNames = Collections.emptyList();
        } else {
            queryGroupNames = evaluation.getChildren().stream()
                    .filter(c -> c.getName().equals(corpus))
                    .flatMap(c -> c.getChildren().stream())
                    .filter(t -> t.getName().equals(topic))
                    .flatMap(t -> t.getChildren().stream())
                    .map(QueryGroup::getName)
                    .collect(Collectors.toList());
        }

        return queryGroupNames;
    }

    @Override
    public Evaluation filterEvaluation(final Collection<String> corpora,
                                       final Collection<DashboardTopic> topics,
                                       final Collection<DashboardQueryGroup> queryGroups,
                                       final Collection<String> metrics,
                                       final Collection<String> versions) {
        final Evaluation eval;

        if (evaluation.getChildren() == null || evaluation.getChildren().isEmpty()) {
            eval = evaluation;
        } else {
            eval = new Evaluation();

            // Gather the queries
            final List<Query> queries = evaluation.getChildren().stream()
                    .filter(c -> corpusMatches(c, corpora))
                    .flatMap(c -> c.getChildren().stream())
                    .filter(t -> topicMatches(t, topics))
                    .flatMap(t -> t.getChildren().stream())
                    .filter(qg -> queryGroupMatches(qg, queryGroups))
                    .flatMap(qg -> qg.getChildren().stream())
                    .collect(Collectors.toList());

            // Build an evaluation by filtering the queries
            queries.forEach(q -> {
                // Ensure the parent hierarchy exists
                Corpus c = eval.findOrCreate(findParentName(q, Corpus.class), Corpus::new);
                Topic t = c.findOrCreate(findParentName(q, Topic.class), Topic::new);
                QueryGroup qg = t.findOrCreate(findParentName(q, QueryGroup.class), QueryGroup::new);

                final Query filteredQuery = filterQueryMetrics(q, qg, metrics, versions);
                // Propagate the metrics up the hierarchy
                filteredQuery.notifyCollectedMetrics();
            });
        }

        return eval;
    }

    private boolean corpusMatches(final Corpus corpus, final Collection<String> corpusNames) {
        return corpusNames == null || corpusNames.isEmpty() || corpusNames.contains(corpus.getName());
    }

    private boolean topicMatches(final Topic topic, final Collection<DashboardTopic> dashboardTopics) {
        boolean ret = false;

        if (dashboardTopics == null || dashboardTopics.isEmpty()) {
            ret = true;
        } else {
            final String corpusName = findParentName(topic, Corpus.class);
            for (DashboardTopic dTopic : dashboardTopics) {
                if (dTopic.getCorpus().equals(corpusName)) {
                    if (dTopic.getTopicName().equals(topic.getName())) {
                        ret = true;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    private boolean queryGroupMatches(final QueryGroup queryGroup, final Collection<DashboardQueryGroup> dashboardQueryGroups) {
        boolean ret = false;

        if (dashboardQueryGroups == null || dashboardQueryGroups.isEmpty()) {
            ret = true;
        } else {
            String topicName = findParentName(queryGroup, Topic.class);
            String corpusName = findParentName(queryGroup, Corpus.class);
            for (DashboardQueryGroup dqg : dashboardQueryGroups) {
                if (dqg.getCorpus().equals(corpusName) && dqg.getTopic().equals(topicName) && dqg.getQueryGroup().equals(queryGroup.getName())) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    private Query filterQueryMetrics(final Query query, final QueryGroup parent, final Collection<String> metricFilter, final Collection<String> versions) {
        final Query q = filterQueryData(query, parent, versions);

        if (metricFilter == null || metricFilter.isEmpty()) {
            // No metric filters passed - use all the metrics
            query.getMetrics().forEach((n, m) -> q.getMetrics().put(n, filterMetricVersions(m, versions)));
        } else {
            for (String metric : metricFilter) {
                ofNullable(query.getMetrics().get(metric))
                        .ifPresent(m -> q.getMetrics().put(metric, filterMetricVersions(m, versions)));
            }
        }

        return q;
    }

    private Query filterQueryData(final Query query, final QueryGroup parent, final Collection<String> versions) {
        final Query q = parent.findOrCreate(query.getName(), Query::new);

        // Loop through the versions from the incoming query - avoids checking
        // incoming versions has content before we start
        query.getMetrics().keySet().forEach(v -> {
            if (versions == null || versions.isEmpty() || versions.contains(v)) {
                ofNullable(query.getResults().get(v)).ifPresent(results -> {
                    q.setTotalHits(results.totalHits(), v);
                    results.hits().forEach(h -> q.collect(h, -1, v));
                });
            }
        });

        return q;
    }

    private Metric filterMetricVersions(final Metric metric, final Collection<String> versions) {
        final Metric m;

        if (versions == null || versions.isEmpty()) {
            // No versions passed in - return the base metric
            m = metric;
        } else {
            // Create a new StaticMetric with just the required versions
            m = new StaticMetric(metric.getName());

            for (final String v : versions) {
                ofNullable(metric.valueFactory(v)).ifPresent(vf -> ((StaticMetric) m).collect(v, vf.value()));
            }
        }

        return m;
    }

    private static String findParentName(DomainMember<?> domainMember, Class<? extends DomainMember> parentClass) {
        String ret = null;

        if (domainMember.getClass().equals(parentClass)) {
            ret = domainMember.getName();
        } else if (domainMember.getParent().isPresent()) {
            ret = findParentName(domainMember.getParent().get(), parentClass);
        }

        return ret;
    }
}
