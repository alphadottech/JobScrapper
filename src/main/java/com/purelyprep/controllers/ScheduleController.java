package com.purelyprep.controllers;

import com.purelyprep.pojo.*;
import com.purelyprep.services.ScheduleService;
import com.purelyprep.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping(value = "/schedules")
    @ResponseBody
    public List<Schedule> getAllSchedules() {
        return scheduleService.getAllSchedules();
    }

    @PostMapping(value = "/schedule")
    @ResponseBody
    public Success addToSchedule(
        @RequestBody Schedule schedule,
        @RequestParam(required = false, name = "initialize") Boolean initialize,
        @RequestParam(required = false, name = "emails") String emails,
        @RequestParam(required = false, name = "setSent") Boolean setSent
    ) {
        scheduleService.addToDailySchedule(schedule);
//        if (initialize != null && initialize) {
//            Set<String> emailSet = null;
//            if (emails != null && !emails.isEmpty()) {
//                emailSet = new HashSet<>();
//                for (String email : emails.split(",")) {
//                    emailSet.add(email.trim());
//                }
//            }
//            scheduleService.scrapeAndEmailInitialJobs(schedule.prefs, emailSet, setSent);
//        }
        return new Success();
    }

    @DeleteMapping(value = "/schedule")
    @ResponseBody
    public Success removeFromSchedule(@RequestBody Schedule schedule) {
        scheduleService.removeFromSchedule(schedule);
        return new Success();
    }

    @GetMapping(value = "/scheduled/results/{candidateId}")
    @ResponseBody
    public ScheduledResults getResults(
        @PathVariable("candidateId") String candidateId,
        @RequestParam(required = false, name = "top") Integer top,
        @RequestParam(required = false, name = "minScore") Integer minScore
    ) {
        ScheduledResults results = scheduleService.getResults(candidateId);
        if (results == null) {
            return null;
        }
        results.jobResult = Util.getTopJobs(results.jobResult, top, minScore);
        return results;
    }

    @GetMapping(value = "/scheduled/results/text/{candidateId}", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getResultsText(
        @PathVariable("candidateId") String candidateId,
        @RequestParam(required = false, name = "top") Integer top,
        @RequestParam(required = false, name = "minScore") Integer minScore
    ) {
        ScheduledResults results = getResults(candidateId, top, minScore);
        if (results == null) {
            return null;
        }
        return "Results as of: [" + results.dateTime + "]\n\n\n" + Util.formatJobs(results.jobResult);
    }

}
