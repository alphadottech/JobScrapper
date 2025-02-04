package com.purelyprep.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.CandidatePreferences;
import com.purelyprep.pojo.JobResult;
import com.purelyprep.pojo.JobScrapeRequest;
import com.purelyprep.util.Util;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static JobService jobService;

    public static JobService getInstance() {
        return jobService;
    }

    @Value("${redis.job.queue}")
    private String jobQueue;
    @Value("${peopledatalabs.api.key}")
    private String apiKey;
    @PostConstruct
    private void init() {
        jobService = this;
    }

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    public JobService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, RedisService redisService,RestTemplate restTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.restTemplate=restTemplate;
    }

    public void enqueueJobScrape(JobScrapeRequest request) {
        try {
            this.stringRedisTemplate.convertAndSend(jobQueue, objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.error("Error serializing: ", e);
        }
    }

    public JobResult getJobs(String runId, Integer top, Integer minScore) {
        JobResult jobResult = redisService.get(CandidatePreferences.getRunKey(runId));
        if (jobResult == null) {
            return null;
        }
        return Util.getTopJobs(jobResult, top, minScore);
    }

    public List<String> getAutocompleteSuggestions(String text) {
        // Construct URL
        String baseUrl = "https://api.peopledatalabs.com/v5/autocomplete";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("field", "title")
                .queryParam("text", text);

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("X-Api-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create HTTP entity
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Make the API call
        try {
            PeopleDataLabsDTO response = restTemplate.exchange(
                    builder.toUriString(), 
                    HttpMethod.GET, 
                    entity, 
                    PeopleDataLabsDTO.class
            ).getBody();

            // Extract and return only names
            return response != null && response.getData() != null 
                ? response.getData().stream()
                    .map(PeopleDataLabsDTO.AutocompleteResult::getName)
                    .collect(Collectors.toList())
                : Collections.emptyList();
        } catch (Exception e) {
            // Log the error and return empty list
            System.err.println("Error fetching autocomplete suggestions: " + e.getMessage());
            return Collections.emptyList();
        }
    }

}