package com.purelyprep.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.CandidatePreferences;
import com.purelyprep.pojo.Job;
import com.purelyprep.pojo.JobResult;
import com.purelyprep.pojo.JobScrapeRequest;
import com.purelyprep.util.Util;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static JobService jobService;

    public static JobService getInstance() {
        return jobService;
    }

    @Value("${redis.job.queue}")
    private String jobQueue;

    @PostConstruct
    private void init() {
        jobService = this;
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    public JobService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, RedisService redisService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
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

}
