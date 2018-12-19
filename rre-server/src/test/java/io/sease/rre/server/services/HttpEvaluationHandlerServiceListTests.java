package io.sease.rre.server.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sease.rre.core.domain.Corpus;
import io.sease.rre.core.domain.Evaluation;
import io.sease.rre.core.domain.QueryGroup;
import io.sease.rre.core.domain.Topic;
import io.sease.rre.core.domain.metrics.Metric;
import io.sease.rre.server.data.DashboardQueryGroup;
import io.sease.rre.server.data.DashboardTopic;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the list fetch methods in the HTTP evaluation handler
 * service.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
public class HttpEvaluationHandlerServiceListTests {

    private static final String EXAMPLE_JSON_FILE = "/http/example_evaluation.json";

    private static final String CORPUS = "electric_basses.bulk";
    private static final String TOPIC = "Fender basses";
    private static final String QUERYGROUP = "The group tests several searches on the Fender brand";

    private static JsonNode exampleJson;

    private EvaluationHandlerService handler;

    @BeforeClass
    public static void initialiseExampleJson() throws Exception {
        exampleJson = new ObjectMapper().readTree(HttpEvaluationHandlerServiceListTests.class.getResourceAsStream(EXAMPLE_JSON_FILE));
    }

    @Before
    public void setupHandler() {
        handler = new HttpEvaluationHandlerService();
    }

    @After
    public void tearDownHandler() {
        handler = null;
    }

    @Test
    public void getMetricsReturnsEmptyList_whenNoEvaluationSet() throws Exception {
        List<String> metrics = handler.getMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics).isEmpty();
    }

    @Test
    public void getMetricsReturnsExpectedList() throws Exception {
        handler.processEvaluationRequest(exampleJson);

        List<String> metrics = handler.getMetrics();
        assertThat(metrics).contains("P", "R", "RR@10", "AP", "NDCG@10", "P@1", "P@2", "P@3", "P@10");
    }

    @Test
    public void getVersionsReturnsEmptyList_whenNoEvaluationSet() throws Exception {
        List<String> versions = handler.getVersions();

        assertThat(versions).isNotNull();
        assertThat(versions).isEmpty();
    }

    @Test
    public void getVersionsReturnsExpectedList() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> versions = handler.getVersions();

        assertThat(versions).contains("v1.0", "v1.1");
    }

    @Test
    public void getCorpusNamesReturnsEmptyList_whenNoEvaluationSet() throws Exception {
        List<String> corpusNames = handler.getCorpusNames();

        assertThat(corpusNames).isNotNull();
        assertThat(corpusNames).isEmpty();
    }

    @Test
    public void getCorpusNamesReturnsExpectedList() throws Exception {
        handler.processEvaluationRequest(exampleJson);

        List<String> corpusNames = handler.getCorpusNames();
        assertThat(corpusNames).containsExactly(CORPUS);
    }

    @Test
    public void getTopicNamesReturnsEmptyList_whenNoEvaluationSet() throws Exception {
        List<String> topicNames = handler.getTopicNames("corpus");

        assertThat(topicNames).isNotNull();
        assertThat(topicNames).isEmpty();
    }

    @Test
    public void getTopicNamesReturnsEmptyList_whenCorpusIsNull() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> topicNames = handler.getTopicNames(null);

        assertThat(topicNames).isNotNull();
        assertThat(topicNames).isEmpty();
    }

    @Test
    public void getTopicNamesReturnsEmptyList_whenCorpusDoesNotExist() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> topicNames = handler.getTopicNames("blah");

        assertThat(topicNames).isNotNull();
        assertThat(topicNames).isEmpty();
    }

    @Test
    public void getTopicNamesReturnsExpectedTopics() throws Exception {
        handler.processEvaluationRequest(exampleJson);

        List<String> topicNames = handler.getTopicNames(CORPUS);

        assertThat(topicNames).containsExactly("Fender basses", "Gibson basses");
    }

    @Test
    public void getQueryGroupNamesReturnsEmptyList_whenNoEvaluationSet() throws Exception {
        List<String> qgNames = handler.getQueryGroupNames("", "");

        assertThat(qgNames).isNotNull();
        assertThat(qgNames).isEmpty();
    }

    @Test
    public void getQueryGroupNamesReturnsEmptyList_whenCorpusIsNull() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> qgNames = handler.getQueryGroupNames(null, "");

        assertThat(qgNames).isNotNull();
        assertThat(qgNames).isEmpty();
    }

    @Test
    public void getQueryGroupNamesReturnsEmptyList_whenTopicIsNull() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> qgNames = handler.getQueryGroupNames("corpus", null);

        assertThat(qgNames).isNotNull();
        assertThat(qgNames).isEmpty();
    }

    @Test
    public void getQueryGroupNamesReturnsEmptyList_whenNoSuchCorpus() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> qgNames = handler.getQueryGroupNames("corpus", TOPIC);

        assertThat(qgNames).isNotNull();
        assertThat(qgNames).isEmpty();
    }

    @Test
    public void getQueryGroupNamesReturnsEmptyList_whenNoSuchTopic() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> qgNames = handler.getQueryGroupNames(CORPUS, "");

        assertThat(qgNames).isNotNull();
        assertThat(qgNames).isEmpty();
    }

    @Test
    public void getQueryGroupNamesReturnsExpectedList() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        List<String> qgNames = handler.getQueryGroupNames(CORPUS, TOPIC);

        assertThat(qgNames.size()).isEqualTo(2);
        assertThat(qgNames).contains("The group tests several searches on the Fender brand", "Several searches on a given model (Jazz bass)");
    }

    @Test
    public void filterEvaluationReturnsEmptyEvaluation_whenNoEvaluationSet() throws Exception {
        Evaluation eval = handler.filterEvaluation(Collections.singletonList(CORPUS),
                Collections.singletonList(new DashboardTopic(TOPIC, CORPUS)),
                Collections.singletonList(new DashboardQueryGroup(QUERYGROUP, TOPIC, CORPUS)),
                Arrays.asList("P", "R"), Arrays.asList("v1.0", "v1.1"));

        assertThat(eval).isNotNull();
        assertThat(eval.getChildren()).isNullOrEmpty();
    }

    @Test
    public void filterEvaluationReturnsFilteredEvaluation() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        Evaluation eval = handler.filterEvaluation(Collections.singletonList(CORPUS),
                Collections.singletonList(new DashboardTopic(TOPIC, CORPUS)),
                Collections.singletonList(new DashboardQueryGroup(QUERYGROUP, TOPIC, CORPUS)),
                Arrays.asList("P", "R", "AP"), Collections.singleton("v1.1"));

        assertThat(eval).isNotNull();
        assertThat(eval.getChildren().size()).isEqualTo(1);

        Corpus c = eval.getChildren().get(0);
        assertThat(c.getName()).isEqualTo(CORPUS);
        assertThat(c.getChildren().size()).isEqualTo(1);

        Topic t = c.getChildren().get(0);
        assertThat(t.getName()).isEqualTo(TOPIC);
        assertThat(t.getChildren().size()).isEqualTo(1);

        QueryGroup qg = t.getChildren().get(0);
        assertThat(qg.getName()).isEqualTo(QUERYGROUP);

        // Check the metrics
        assertThat(eval.getMetrics().size()).isEqualTo(3);
        assertThat(eval.getMetrics().keySet()).contains("P", "R", "AP");
        Metric m = eval.getMetrics().values().iterator().next();
        assertThat(m.getVersions().size()).isEqualTo(1);
        assertThat(m.getVersions().keySet()).containsExactly("v1.1");
    }

    @Test
    public void filterEvaluationReturnsFilteredEvaluation_withMissingParams() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        Evaluation eval = handler.filterEvaluation(null, Collections.singletonList(new DashboardTopic(TOPIC, CORPUS)), null, Arrays.asList("P", "R", "AP"), Collections.singleton("v1.0"));

        assertThat(eval).isNotNull();
        assertThat(eval.getChildren().size()).isEqualTo(1);

        // Only one corpus in the eval file
        Corpus c = eval.getChildren().get(0);
        // Check that only one child (topic) returned
        assertThat(c.getChildren().size()).isEqualTo(1);

        // Check that the topic is the one we asked for
        Topic t = c.getChildren().get(0);
        assertThat(t.getName()).isEqualTo(TOPIC);

        // Check the metrics
        assertThat(eval.getMetrics().size()).isEqualTo(3);
        assertThat(eval.getMetrics().keySet()).contains("P", "R", "AP");
        Metric m = eval.getMetrics().values().iterator().next();
        assertThat(m.getVersions().size()).isEqualTo(1);
        assertThat(m.getVersions().keySet()).containsExactly("v1.0");
    }

    @Test
    public void filterEvaluationReturnsAllVersions_withMissingVersionParam() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        Evaluation eval = handler.filterEvaluation(Collections.singletonList(CORPUS),
                Collections.singletonList(new DashboardTopic(TOPIC, CORPUS)),
                Collections.singletonList(new DashboardQueryGroup(QUERYGROUP, TOPIC, CORPUS)),
                Arrays.asList("P", "R", "AP"), null);

        assertThat(eval).isNotNull();
        assertThat(eval.getChildren().size()).isEqualTo(1);

        Corpus c = eval.getChildren().get(0);
        assertThat(c.getName()).isEqualTo(CORPUS);
        assertThat(c.getChildren().size()).isEqualTo(1);

        Topic t = c.getChildren().get(0);
        assertThat(t.getName()).isEqualTo(TOPIC);
        assertThat(t.getChildren().size()).isEqualTo(1);

        QueryGroup qg = t.getChildren().get(0);
        assertThat(qg.getName()).isEqualTo(QUERYGROUP);

        // Check the metrics
        assertThat(eval.getMetrics().size()).isEqualTo(3);
        assertThat(eval.getMetrics().keySet()).contains("P", "R", "AP");
        Metric m = eval.getMetrics().values().iterator().next();
        assertThat(m.getVersions().size()).isEqualTo(2);
        assertThat(m.getVersions().keySet()).containsExactly("v1.0", "v1.1");
    }

    @Test
    public void filterEvaluationReturnsAllMetrics_withMissingMetricParam() throws Exception {
        handler.processEvaluationRequest(exampleJson);
        Evaluation eval = handler.filterEvaluation(Collections.singletonList(CORPUS),
                Collections.singletonList(new DashboardTopic(TOPIC, CORPUS)),
                Collections.singletonList(new DashboardQueryGroup(QUERYGROUP, TOPIC, CORPUS)),
                null, Collections.singleton("v1.1"));

        assertThat(eval).isNotNull();
        assertThat(eval.getChildren().size()).isEqualTo(1);

        Corpus c = eval.getChildren().get(0);
        assertThat(c.getName()).isEqualTo(CORPUS);
        assertThat(c.getChildren().size()).isEqualTo(1);

        Topic t = c.getChildren().get(0);
        assertThat(t.getName()).isEqualTo(TOPIC);
        assertThat(t.getChildren().size()).isEqualTo(1);

        QueryGroup qg = t.getChildren().get(0);
        assertThat(qg.getName()).isEqualTo(QUERYGROUP);

        // Check the metrics
        assertThat(eval.getMetrics().size()).isEqualTo(9);
        assertThat(eval.getMetrics().keySet()).contains("P", "R", "RR@10", "AP", "NDCG@10", "P@1", "P@2", "P@3", "P@10");
        Metric m = eval.getMetrics().values().iterator().next();
        assertThat(m.getVersions().size()).isEqualTo(1);
        assertThat(m.getVersions().keySet()).containsExactly("v1.1");
    }
}
