package com.purelyprep.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.*;
import com.purelyprep.services.JobService;
import com.purelyprep.services.RedisService;
import com.purelyprep.services.ScheduleService;
import com.purelyprep.util.Util;

import jakarta.validation.constraints.Pattern;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.ClientInfoStatus;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;

    private final ScheduleService scheduleService;

    private RedisService redisService;

    public JobController(JobService jobService, ScheduleService scheduleService, RedisService redisService) {
        this.jobService = jobService;
        this.scheduleService=scheduleService;
        this.redisService=redisService;
    }

    @PostMapping(value = "/jobs")
    @ResponseBody
    public ResponseMessage<JobScrapeRequest> getJobs(@RequestBody JobScrapeRequest request)
    {
        request.prefs.runId = UUID.randomUUID().toString();
        jobService.enqueueJobScrape(request);
        return new ResponseMessage<>("success", "success", request);
    }

    @GetMapping("/getUserVerification")
    public ResponseEntity<Object> getUserVerification(@RequestParam (required = true, name = "userEmail") String userEmail) throws JsonProcessingException {
        boolean userExist=false;
        String key="";
        List<String> prepareListOfKeys= constructRedisKeys(userEmail);
        ListIterator<String> stringListIterator = prepareListOfKeys.listIterator();
        while (stringListIterator.hasNext()){
           key= stringListIterator.next();
           userExist = redisService.exists(key);
        }
        if (!userExist){
            log.info("User Email not found....");
           // return new ResponseEntity<>("No Record found", HttpStatusCode.valueOf(HttpStatus.SC_OK)) ;
            return ResponseEntity.ok().body(new ResponseDTO(HttpStatus.SC_NO_CONTENT ,"User Not Found"));
        } else {
            Object redisValueObject = redisService.get(key);
            log.info("User Email address found....");
            return new ResponseEntity<>(redisValueObject, HttpStatusCode.valueOf(HttpStatus.SC_OK)) ;
        }
    }

    private List<String> constructRedisKeys(String userEmail) {
        List<String> list = new ArrayList<>();
        list.add("scheduled-run-"+userEmail);
        //list.add("scheduled-daily-set-Scheduled-set-5")
        return list;
    }


    @GetMapping(value = "/results/{runId}")
    @ResponseBody
    public ResponseMessage<JobResult> getResults(
        @PathVariable("runId") String runId,
        @RequestParam(required = false, name = "top") Integer top,
        @RequestParam(required = false, name = "minScore") Integer minScore
    ) {
        return new ResponseMessage<>(jobService.getJobs(runId, top, minScore));
    }

    @GetMapping(value = "/results/text/{runId}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getResultsText(
        @PathVariable("runId") String runId,
        @RequestParam(required = false, name = "top") Integer top,
        @RequestParam(required = false, name = "minScore") Integer minScore
    ) {
        ResponseMessage<JobResult> resp = getResults(runId, top, minScore);
        if (resp == null) {
            return null;
        }
        return Util.formatJobs(resp.response);
    }
    
	   @GetMapping("/getSuggestions/{title}")
	    public ResponseEntity<List<String>> getAutocompleteSuggestions(
	            @PathVariable(name = "title") String title) {
	        
	        List<String> suggestions = jobService.getAutocompleteSuggestions(title);
	        return ResponseEntity.ok(suggestions);
	    }

}
