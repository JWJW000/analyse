package com.ethicssra.service.llm;

import com.ethicssra.config.DashScopeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashScopeGateway {

    private static final Logger log = LoggerFactory.getLogger(DashScopeGateway.class);

    private final RestTemplate restTemplate;
    private final DashScopeConfig config;
    private final ObjectMapper objectMapper;

    public DashScopeGateway(RestTemplate restTemplate, DashScopeConfig config, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, null);
    }

    public String chat(String systemPrompt, String userMessage, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        if (history != null) {
            for (ChatMessage msg : history) {
                messages.add(Map.of("role", msg.role, "content", msg.content));
            }
        }

        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModel());
        body.put("input", Map.of("messages", messages));
        body.put("parameters", Map.of(
            "max_tokens", config.getMaxTokens(),
            "temperature", config.getTemperature(),
            "result_format", "message"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());
        headers.set("Accept", "application/json");

        try {
            String url = config.getBaseUrl() + "/services/aigc/text-generation/generation";
            Map<String, Object> response = restTemplate.postForObject(url,
                new org.springframework.http.HttpEntity<>(body, headers),
                Map.class);

            if (response == null) {
                throw new RuntimeException("Empty response from DashScope API");
            }

            JsonNode output = objectMapper.valueToTree(response).get("output");
            if (output == null) {
                throw new RuntimeException("Missing 'output' in DashScope response");
            }

            JsonNode choices = output.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Missing or empty 'choices' in DashScope response");
            }

            return choices.get(0).get("message").get("content").asText();

        } catch (Exception e) {
            log.error("DashScope API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    public static class ChatMessage {
        public final String role;
        public final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
