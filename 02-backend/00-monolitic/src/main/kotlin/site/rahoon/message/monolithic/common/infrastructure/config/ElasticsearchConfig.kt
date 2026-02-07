package site.rahoon.message.monolithic.common.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

/**
 * Elasticsearch Configuration for Spring Data Elasticsearch.
 *
 * This configuration class enables Elasticsearch repositories and sets up the
 * necessary infrastructure for connecting to and interacting with Elasticsearch.
 *
 * ## How it works:
 * 1. Spring Boot auto-configures the ElasticsearchClient based on application.properties
 * 2. @EnableElasticsearchRepositories scans for repository interfaces extending ElasticsearchRepository
 * 3. Spring Data Elasticsearch provides implementations at runtime
 *
 * ## Configuration Properties (from application.properties):
 * - spring.elasticsearch.uris: Elasticsearch server URL (default: http://localhost:9200)
 * - spring.elasticsearch.connection-timeout: Connection timeout (default: 5s)
 * - spring.elasticsearch.socket-timeout: Socket timeout (default: 30s)
 *
 * ## Package Structure:
 * - Document classes: Represent Elasticsearch documents (like JPA entities)
 * - Repository interfaces: Define data access operations
 * - Service classes: Business logic layer
 *
 * @see org.springframework.data.elasticsearch.repository.ElasticsearchRepository
 */
@Configuration
@EnableElasticsearchRepositories(
    // Scan for Elasticsearch repositories in the search infrastructure package
    basePackages = ["site.rahoon.message.monolithic.message.search.infrastructure"]
)
class ElasticsearchConfig
// Spring Boot auto-configures ElasticsearchClient from application.properties
// No additional bean definitions needed for basic setup
