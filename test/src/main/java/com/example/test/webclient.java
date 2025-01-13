package com.example.test;

public class webclient {
}
package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Map;

@Service
public class QueryAIService {

    @Value("${openai.api.key}")
    private String openAIApiKey;

    @Value("${openai.api.url}")
    private String openAIUrl;

    @Value("${proxy.host}")
    private String proxyHost;

    @Value("${proxy.port}")
    private int proxyPort;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public QueryAIService() {
        this.objectMapper = new ObjectMapper();

        // Configure WebClient with proxy
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                                .address(new InetSocketAddress(proxyHost, proxyPort)))))
                .baseUrl(openAIUrl)
                .defaultHeader("Authorization", "Bearer " + openAIApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String queryAI(String systemPrompt, String userPrompt, boolean jsonMode) throws Exception {
        // Create request body
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        if (jsonMode) {
            ((Map<String, Object>) requestBody).put("response_format", Map.of("type", "json_object"));
        }

        // Send request and get response
        String response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to query AI: " + e.getMessage())))
                .block();

        // Parse and return the content
        JsonNode jsonResponse = objectMapper.readTree(response);
        return jsonResponse.path("choices").get(0).path("message").path("content").asText();
    }
}
