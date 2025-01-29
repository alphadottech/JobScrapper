package com.purelyprep.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.ChatRequest;
import com.purelyprep.pojo.ChatResponse;
import com.purelyprep.pojo.Message;
import com.purelyprep.util.Util;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private static final int maxRetries = 10;

    private static OpenAiService openAiService;
    public static OpenAiService getInstance() {
        return openAiService;
    }


    private final RestTemplate restTemplate;

    @PostConstruct
    private void init() {
        openAiService = this;
    }

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chat(List<Message> messages) {
        return chat(messages, 0);
    }

    public String chat(List<Message> messages, int retries) {
        if (retries > maxRetries) {
            log.error("Exhausted chat retries");
            return "";
        }

        try {
            ChatRequest request = new ChatRequest(model, messages);
            String reqStr = this.objectMapper.writeValueAsString(request);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer " + openAiApiKey);
            HttpEntity<String> entity = new HttpEntity<>(reqStr, headers);

            ResponseEntity<ChatResponse> respEntity = restTemplate.postForEntity(apiUrl, entity, ChatResponse.class);
            if (respEntity.getStatusCode().value() == 429) { // too many requests, wait a bit and try again
                log.info("Too many requests. Trying again...");
                Util.waitRandom(1000, 3000);
                return chat(messages, retries + 1);
            }

            ChatResponse response = respEntity.getBody();
            if (response == null || response.choices == null || response.choices.isEmpty()) {
                return "";
            }

            return response.choices.get(0).message.content;
        } catch (HttpClientErrorException.TooManyRequests e) {
            if (e.getMessage().contains("You exceeded your current quota")) {
                log.error("Quota exceeded");
                return "";
            }
            log.info("Too many requests: [" + e.getMessage() + "]. Trying again...");
            Util.waitRandom(1000, 3000);
            return chat(messages, retries + 1);
        } catch (Exception e) {
            log.error("Error sending chat: ", e);
            return "";
        }
    }

}
