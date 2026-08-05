package com.app.decisioniq.infrastructure.catalog;

import com.app.decisioniq.api.filter.RequestIdentityFilter;
import com.app.decisioniq.application.catalog.CatalogSearchUnavailableException;
import com.app.decisioniq.config.catalog.CatalogRelevanceProperties;
import com.app.decisioniq.domain.catalog.CatalogRelevanceDecision.Match;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(
            RestClient.Builder builder,
            CatalogRelevanceProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public List<Match> search(String question, String correlationId) {
        try {
            SearchResponse response = restClient.post()
                    .uri("/internal/v1/catalog/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(RequestIdentityFilter.CORRELATION_HEADER, correlationId)
                    .body(new SearchRequest(question))
                    .retrieve()
                    .body(SearchResponse.class);
            if (response == null || response.hits() == null) {
                return List.of();
            }
            return response.hits().stream()
                    .map(hit -> new Match(hit.catalogKey(), hit.version(), hit.score()))
                    .toList();
        } catch (RestClientException exception) {
            throw new CatalogSearchUnavailableException(
                    "Catalog relevance service is unavailable", exception
            );
        }
    }

    private record SearchRequest(String query) { }

    private record SearchResponse(List<SearchHit> hits) { }

    private record SearchHit(String catalogKey, int version, double score) { }
}
