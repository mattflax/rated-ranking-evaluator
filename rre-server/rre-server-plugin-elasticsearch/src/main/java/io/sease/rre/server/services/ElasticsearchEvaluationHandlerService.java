package io.sease.rre.server.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sease.rre.core.domain.*;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.server.config.ElasticsearchConfiguration;
import io.sease.rre.server.data.DashboardQueryGroup;
import io.sease.rre.server.data.DashboardTopic;
import io.sease.rre.server.data.ElasticsearchQueryResult;
import io.sease.rre.server.domain.EvaluationMetadata;
import io.sease.rre.server.domain.StaticMetric;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elasticsearch implementation of the evaluation handler service.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@Service
@Profile("elasticsearch")
public class ElasticsearchEvaluationHandlerService implements EvaluationHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchEvaluationHandlerService.class);

    private static final String METRICS_FIELD = "metrics.name";
    private static final String VERSION_FIELD = "version";
    private static final String CORPUS_FIELD = "corpora";
    private static final String TOPIC_FIELD = "topic.keyword";
    private static final String QUERY_GROUP_FIELD = "queryGroup.keyword";

    private static final String[] BASE_SOURCE_FIELDS = new String[]{
            "id", CORPUS_FIELD, "topic", "queryGroup", "queryText", "metrics.*", "version", "totalHits"
    };

    private final ElasticsearchConfiguration configuration;
    private final RestHighLevelClient restHighLevelClient;

    private Evaluation evaluation = new Evaluation();
    private EvaluationMetadata metadata = new EvaluationMetadata(Collections.emptyList(), Collections.emptyList());

    private ElasticsearchEvaluationUpdater updater = null;

    @Autowired
    public ElasticsearchEvaluationHandlerService(ElasticsearchConfiguration configuration) {
        LOGGER.info("Initialising ES Evaluation Handler to use {} - index {}", configuration.getUrl(), configuration.getIndex());
        this.configuration = configuration;
        this.restHighLevelClient = new RestHighLevelClient(RestClient.builder(
                HttpHost.create(configuration.getUrl())));
    }

    @Override
    public void processEvaluationRequest(JsonNode requestData) throws EvaluationHandlerException {
        if (updater != null && updater.isAlive()) {
            throw new EvaluationHandlerException("Update is already running - request rejected!");
        }

        updater = new ElasticsearchEvaluationUpdater(configuration.getIndex(),
                findEvaluationName(requestData, configuration.getIndex()));
        updater.setDaemon(true);
        updater.start();
    }

    @Override
    public Evaluation getEvaluation() {
        return evaluation;
    }

    @Override
    public EvaluationMetadata getEvaluationMetadata() {
        return metadata;
    }

    private String findEvaluationName(JsonNode requestData, String defaultValue) {
        final String evaluationName;

        JsonNode evalNameNode = requestData.findValue("evaluationName");
        if (evalNameNode != null) {
            evaluationName = evalNameNode.asText();
        } else {
            evaluationName = defaultValue;
        }

        return evaluationName;
    }

    @Override
    public List<String> getMetrics() throws EvaluationHandlerException {
        return getTermsAggregation(METRICS_FIELD);
    }

    @Override
    public List<String> getVersions() throws EvaluationHandlerException {
        return getTermsAggregation(VERSION_FIELD);
    }

    @Override
    public List<String> getCorpusNames() throws EvaluationHandlerException {
        return getTermsAggregation(CORPUS_FIELD);
    }

    private List<String> getTermsAggregation(final String termsField) throws EvaluationHandlerException {
        final List<String> terms;

        try {
            final String aggName = "terms_agg";

            SearchSourceBuilder ssb = new SearchSourceBuilder().size(0)
                    .aggregation(AggregationBuilders.terms(aggName).field(termsField).size(100));
            SearchRequest searchRequest = new SearchRequest(configuration.getIndex()).source(ssb);
            SearchResponse response = restHighLevelClient.search(searchRequest);

            terms = extractKeysFromTermsAggregation(response.getAggregations().get(aggName));
        } catch (IOException e) {
            LOGGER.error("IO Exception retrieving terms for {}: {}", termsField, e.getMessage());
            throw new EvaluationHandlerException(e);
        }

        return terms;
    }

    private List<String> extractKeysFromTermsAggregation(final Terms termsAggregation) {
        return termsAggregation.getBuckets().stream()
                .map(MultiBucketsAggregation.Bucket::getKey)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTopicNames(String corpus) throws EvaluationHandlerException {
        final String aggName = "topics_agg";
        final List<String> topics;

        try {
            SearchSourceBuilder ssb = new SearchSourceBuilder().size(0)
                    .query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery(CORPUS_FIELD, corpus)))
                    .aggregation(AggregationBuilders.terms(aggName).field(TOPIC_FIELD).size(100));
            SearchRequest request = new SearchRequest(configuration.getIndex()).source(ssb);
            SearchResponse response = restHighLevelClient.search(request);

            topics = extractKeysFromTermsAggregation(response.getAggregations().get(aggName));
        } catch (IOException e) {
            LOGGER.error("IO Exception retrieving topics: {}", e.getMessage());
            throw new EvaluationHandlerException(e);
        }

        return topics;
    }

    @Override
    public List<String> getQueryGroupNames(String corpus, String topic) throws EvaluationHandlerException {
        final String aggName = "queryGroup_agg";
        final List<String> queryGroups;

        try {
            SearchSourceBuilder ssb = new SearchSourceBuilder().size(0)
                    .query(QueryBuilders.boolQuery().filter(
                            QueryBuilders.boolQuery()
                                    .must(QueryBuilders.termQuery(CORPUS_FIELD, corpus))
                                    .must(QueryBuilders.termQuery(TOPIC_FIELD, topic))))
                    .aggregation(AggregationBuilders.terms(aggName).field(QUERY_GROUP_FIELD).size(100));
            SearchRequest request = new SearchRequest(configuration.getIndex()).source(ssb);
            SearchResponse response = restHighLevelClient.search(request);

            queryGroups = extractKeysFromTermsAggregation(response.getAggregations().get(aggName));
        } catch (IOException e) {
            LOGGER.error("IO Exception retrieving query groups: {}", e.getMessage());
            throw new EvaluationHandlerException(e);
        }

        return queryGroups;
    }

    @Override
    public Evaluation filterEvaluation(Collection<String> corpora, Collection<DashboardTopic> topics,
                                       Collection<DashboardQueryGroup> queryGroups, Collection<String> metrics,
                                       Collection<String> versions) throws EvaluationHandlerException {
        final Evaluation evaluation;

        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        if (versions != null) {
            final BoolQueryBuilder versionQuery = QueryBuilders.boolQuery();
            versions.forEach(v -> versionQuery.should(QueryBuilders.termQuery(VERSION_FIELD, v)));
            versionQuery.minimumShouldMatch(1);
            filterQuery.must(versionQuery);
        }

        if (queryGroups != null && !queryGroups.isEmpty()) {
            final BoolQueryBuilder queryGroupQuery = QueryBuilders.boolQuery();
            queryGroups.forEach(qg -> queryGroupQuery.should(
                    QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery(CORPUS_FIELD, qg.getCorpus()))
                            .must(QueryBuilders.termQuery(TOPIC_FIELD, qg.getTopic()))
                            .must(QueryBuilders.termQuery(QUERY_GROUP_FIELD, qg.getQueryGroup()))));
            queryGroupQuery.minimumShouldMatch(1);
            filterQuery.must(queryGroupQuery);
        } else if (topics != null && !topics.isEmpty()) {
            final BoolQueryBuilder topicQuery = QueryBuilders.boolQuery();
            topics.forEach(t -> topicQuery.should(
                    QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery(CORPUS_FIELD, t.getCorpus()))
                            .must(QueryBuilders.termQuery(TOPIC_FIELD, t.getTopicName()))));
            topicQuery.minimumShouldMatch(1);
            filterQuery.must(topicQuery);
        } else if (corpora != null && !corpora.isEmpty()) {
            final BoolQueryBuilder corpusQuery = QueryBuilders.boolQuery();
            corpora.forEach(c -> corpusQuery.should(
                    QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery(CORPUS_FIELD, c))));
            corpusQuery.minimumShouldMatch(1);
            filterQuery.must(corpusQuery);
        }

        try {
            SearchSourceBuilder ssb = new SearchSourceBuilder().size(5000)
                    .query(filterQuery)
                    .fetchSource(BASE_SOURCE_FIELDS, new String[0])
                    .sort(VERSION_FIELD, SortOrder.ASC);
            SearchRequest request = new SearchRequest(configuration.getIndex()).source(ssb);
            SearchResponse response = restHighLevelClient.search(request);

            evaluation = buildEvaluationFromSearchResponse(response, configuration.getIndex(), metrics);
        } catch (IOException e) {
            LOGGER.error("IO Exception filtering evaluation data: {}", e.getMessage());
            throw new EvaluationHandlerException(e);
        }

        return evaluation;
    }

    /**
     * Extract an evaluation from a search response, optionally filtering the
     * returned metrics to just those in a given list.
     *
     * @param response        the search response holding the data.
     * @param evaluationName  the name to give the resulting evaluation.
     * @param requiredMetrics a list of metric names that are required. If
     *                        {@code null} or empty, all metrics will be
     *                        returned.
     * @return the resulting evaluation.
     * @throws IOException if the response data cannot be deserialized.
     */
    private static Evaluation buildEvaluationFromSearchResponse(SearchResponse response, String evaluationName,
                                                                Collection<String> requiredMetrics) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Evaluation eval = new Evaluation();
        eval.setName(evaluationName);

        for (SearchHit hit : response.getHits().getHits()) {
            final ElasticsearchQueryResult queryResult = mapper.readValue(hit.getSourceAsString(), ElasticsearchQueryResult.class);

            // Fetch or create the query hierarchy
            Corpus corpus = eval.findOrCreate(queryResult.getCorpora(), Corpus::new);
            Topic topic = corpus.findOrCreate(queryResult.getTopic(), Topic::new);
            QueryGroup queryGroup = topic.findOrCreate(queryResult.getQueryGroup(), QueryGroup::new);
            Query query = queryGroup.findOrCreate(queryResult.getQueryText(), Query::new);
            query.setTotalHits(queryResult.getTotalHits(), queryResult.getVersion());

            // Extract all the metrics
            queryResult.getMetrics().stream()
                    .filter(qm -> metricIsRequired(qm.getName(), requiredMetrics))
                    .forEach(qm -> {
                        final Metric m = query.getMetrics().computeIfAbsent(qm.getName(), k -> new StaticMetric(qm.getName()));
                        ((StaticMetric) m).collect(queryResult.getVersion(), new BigDecimal(qm.getValue()).setScale(4, RoundingMode.CEILING));
                    });

            // And propagate them up through the hierarchy
            query.notifyCollectedMetrics();
        }

        return eval;
    }

    private static boolean metricIsRequired(String metricName, Collection<String> requiredMetrics) {
        return requiredMetrics == null || requiredMetrics.isEmpty() || requiredMetrics.contains(metricName);
    }

    class ElasticsearchEvaluationUpdater extends Thread {

        private final String index;
        private final String evaluationName;

        ElasticsearchEvaluationUpdater(String idx, String evalName) {
            this.index = idx;
            this.evaluationName = evalName;
        }

        @Override
        public void run() {
            try {
                SearchResponse response = findAllQueries(index);
                evaluation = buildEvaluationFromSearchResponse(response, evaluationName, null);
                metadata = HttpEvaluationHandlerService.extractEvaluationMetadata(evaluation);
            } catch (IOException e) {
                LOGGER.error("Caught IOException building evaluation from Elasticsearch: {}", e.getMessage());
            }
        }

        private SearchResponse findAllQueries(String index) throws IOException {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .size(5000)
                    .fetchSource(BASE_SOURCE_FIELDS, new String[0])
                    .query(QueryBuilders.matchAllQuery())
                    .sort(VERSION_FIELD, SortOrder.ASC);
            SearchRequest request = new SearchRequest(index).source(searchSourceBuilder);

            return restHighLevelClient.search(request);
        }
    }
}
