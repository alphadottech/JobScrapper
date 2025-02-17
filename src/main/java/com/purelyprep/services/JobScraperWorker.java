package com.purelyprep.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.Job;
import com.purelyprep.pojo.JobResult;
import com.purelyprep.pojo.JobScrapeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class JobScraperWorker {

    private static final Logger log = LoggerFactory.getLogger(JobScraperWorker.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public JobScraperWorker(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public void receiveMessage(String message) {
        try {
            JobScrapeRequest request = objectMapper.readValue(message, JobScrapeRequest.class);
            JobScraper jobScraper = new JobScraper(JobScraper.MAX_JOBS, restTemplate);
            Map<String, JobResult> jobResult = jobScraper.getJobsForCandidate(request.prefs, request.setSent);
            RedisService.getInstance().set(request.prefs.getRunKey(), jobResult.get("included"), 8, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Error deserializing: ", e);
        }
    }

}
