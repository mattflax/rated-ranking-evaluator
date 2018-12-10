package io.sease.rre.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration details for Elasticsearch.
 *
 * @author Matt Pearce (matt@flax.co.uk)
 */
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@Profile("elasticsearch")
public class ElasticsearchConfiguration {

    private String url = "http://localhost:9200";

    private String index = "rre";

    public String getUrl() {
        return url;
    }

    public String getIndex() {
        return index;
    }
}
