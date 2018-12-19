package io.sease.rre.server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.sease.rre.core.domain.Evaluation;
import io.sease.rre.server.data.DashboardFilterData;
import io.sease.rre.server.domain.EvaluationMetadata;
import io.sease.rre.server.services.EvaluationHandlerException;
import io.sease.rre.server.services.EvaluationHandlerService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/evaluation")
public class RREController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RREController.class);

    @Autowired
    private EvaluationHandlerService evaluationHandler;

    @PostMapping
    public void updateEvaluationData(@RequestBody final JsonNode requestBody) throws Exception {
        evaluationHandler.processEvaluationRequest(requestBody);
    }

    public EvaluationMetadata getMetadata() {
        return evaluationHandler.getEvaluationMetadata();
    }

    @ApiOperation(value = "Returns the evaluation data.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(produces = {"application/json"})
    @ResponseBody
    public Evaluation getEvaluationData() {
        return evaluationHandler.getEvaluation();
    }

    @ApiOperation(value = "Returns the list of available metrics.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(value = "/metricList", produces = {"application/json"})
    @ResponseBody
    public List<String> getMetricList() {
        try {
            return evaluationHandler.getMetrics();
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException fetching available metrics: {}", e);
            return Collections.emptyList();
        }
    }

    @ApiOperation(value = "Returns the list of available versions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(value = "/versionList", produces = {"application/json"})
    @ResponseBody
    public List<String> getVersionList() {
        try {
            return evaluationHandler.getVersions();
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException fetching available versions: {}", e);
            return Collections.emptyList();
        }
    }

    @ApiOperation(value = "Returns the list of available corpora.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(value = "/corpusList", produces = {"application/json"})
    @ResponseBody
    public List<String> getCorpusList() {
        try {
            return evaluationHandler.getCorpusNames();
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException fetching available corpora: {}", e);
            return Collections.emptyList();
        }
    }

    @ApiOperation(value = "Returns the list of available topics for a corpus.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(value = "/topicList", produces = {"application/json"})
    @ResponseBody
    public List<String> getTopicList(@RequestParam(name = "corpus", required = true) String corpus) {
        try {
            return evaluationHandler.getTopicNames(corpus);
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException fetching available topics: {}", e);
            return Collections.emptyList();
        }
    }

    @ApiOperation(value = "Returns the list of available query groups for a topic in a corpus.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @GetMapping(value = "/queryGroupList", produces = {"application/json"})
    @ResponseBody
    public List<String> getQueryGroupList(@RequestParam(name = "corpus", required = true) String corpus,
                                          @RequestParam(name = "topic", required = true) String topic) {
        try {
            return evaluationHandler.getQueryGroupNames(corpus, topic);
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException fetching available query groups: {}", e);
            return Collections.emptyList();
        }
    }

    @ApiOperation(value = "Filters an evaluation by corpus, topic, query group, versions and metrics")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Method successfully returned the evaluation data."),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 414, message = "Request-URI Too Long"),
            @ApiResponse(code = 500, message = "System internal failure occurred.")
    })
    @PostMapping(value = "/filter", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public Evaluation getFilteredEvaluation(@RequestBody DashboardFilterData filterData) {
        try {
            return evaluationHandler.filterEvaluation(filterData.getCorpora(), filterData.getTopics(), filterData.getQueryGroups(), filterData.getMetrics(), filterData.getVersions());
        } catch (EvaluationHandlerException e) {
            LOGGER.error("Caught EvaluationHandlerException filtering the evaluation: {}", e);
            return new Evaluation();
        }
    }
}
