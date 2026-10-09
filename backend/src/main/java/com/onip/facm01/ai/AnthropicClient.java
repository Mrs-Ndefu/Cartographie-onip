package com.onip.facm01.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

// Petit client pour l'API Messages d'Anthropic (https://api.anthropic.com/v1/messages),
// volontairement minimal : un seul message utilisateur, une réponse texte attendue en un mot
// (OUI/NON). Pas besoin du SDK officiel pour ce seul usage.
@Component
class AnthropicClient {

    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_RESPONSE_TOKENS = 8;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    AnthropicClient(
            @Value("${app.ai.address-check.anthropic-api-key:}") String apiKey,
            @Value("${app.ai.address-check.model:claude-haiku-4-5-20251001}") String model) {
        this.apiKey = apiKey;
        this.model = model;

        // Timeouts courts : cet appel se fait toujours dans un thread à part, après la synchro
        // (cf. AddressAiCheckService), mais il ne faut pas pour autant laisser un thread bloqué
        // indéfiniment si l'API Anthropic ne répond pas.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .requestFactory(requestFactory)
                .build();
    }

    boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Renvoie true si l'IA juge le texte fourni dénué de sens / incohérent pour une adresse.
    String ask(String prompt) {
        AnthropicResponse response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AnthropicRequest(model, MAX_RESPONSE_TOKENS, List.of(new AnthropicMessage("user", prompt))))
                .retrieve()
                .body(AnthropicResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            return "";
        }
        String text = response.content().get(0).text();
        return text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
    }

    private record AnthropicMessage(String role, String content) {
    }

    private record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            List<AnthropicMessage> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnthropicResponse(List<AnthropicContentBlock> content) {
    }
}
