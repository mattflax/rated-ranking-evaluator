package io.sease.rre.server.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sease.rre.core.domain.*;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.server.config.ElasticsearchConfiguration;
import io.sease.rre.server.domain.EvaluationMetadata;
import io.sease.rre.server.domain.StaticMetric;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * Elasticsearch implementation of the evaluation handler service.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@Service
@Profile("elasticsearch")
public class ElasticsearchEvaluationHandlerService implements EvaluationHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchEvaluationHandlerService.class);

    private final ElasticsearchConfiguration configuration;
    private final RestHighLevelClient restHighLevelClient;

    private Evaluation evaluation = new Evaluation();
    private EvaluationMetadata metadata = new EvaluationMetadata(Collections.emptyList(), Collections.emptyList());

    private ElasticsearchEvaluationUpdater updater = null;

    @Autowired
    public ElasticsearchEvaluationHandlerService(ElasticsearchConfiguration configuration) {
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
                ObjectMapper mapper = new ObjectMapper();

                Evaluation eval = new Evaluation();
                eval.setName(evaluationName);

                for (SearchHit hit : response.getHits().getHits()) {
                    final QueryResult queryResult = mapper.readValue(hit.getSourceAsString(), QueryResult.class);

                    // Fetch or create the query hierarchy
                    Corpus corpus = eval.findOrCreate(queryResult.getCorpora(), Corpus::new);
                    Topic topic = corpus.findOrCreate(queryResult.getTopic(), Topic::new);
                    QueryGroup queryGroup = topic.findOrCreate(queryResult.getQueryGroup(), QueryGroup::new);
                    Query query = queryGroup.findOrCreate(queryResult.getQueryText(), Query::new);
                    query.setTotalHits(queryResult.getTotalHits(), queryResult.getVersion());

                    // Extract all the metrics
                    queryResult.getMetrics().forEach(qm -> {
                        final Metric m = query.getMetrics().computeIfAbsent(qm.getName(), k -> new StaticMetric(qm.getName()));
                        ((StaticMetric) m).collect(queryResult.getVersion(), new BigDecimal(qm.getValue()).setScale(4, RoundingMode.CEILING));
                    });

                    // And propagate them up through the hierarchy
                    query.notifyCollectedMetrics();
                }

                evaluation = eval;
                metadata = HttpEvaluationHandlerService.extractEvaluationMetadata(eval);
            } catch (IOException e) {
                LOGGER.error("Caught IOException building evaluation from Elasticsearch: {}", e.getMessage());
            }
        }

        private SearchResponse findAllQueries(String index) throws IOException {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .size(5000)
                    .fetchSource(new String[]{ "id", "corpora", "topic", "queryGroup", "queryText", "metrics.*", "version", "totalHits" }, new String[0])
                    .query(QueryBuilders.matchAllQuery())
                    .sort("version", SortOrder.ASC);
            SearchRequest request = new SearchRequest(index).source(searchSourceBuilder);

            return restHighLevelClient.search(request);
        }
    }


    public static class QueryResult {

        private final String id;
        private final String corpora;
        private final String topic;
        private final String queryGroup;
        private final String queryText;
        private final String version;
        private final long totalHits;
        private final List<QueryMetric> metrics;

        public QueryResult(@JsonProperty("id") String id,
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
    }

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
