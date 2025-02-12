package com.purelyprep.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.Schedule;
import com.purelyprep.pojo.ScheduledResults;
import com.purelyprep.pojo.Success;
import com.purelyprep.services.JobService;
import com.purelyprep.services.ScheduleService;
import com.purelyprep.util.Util;

@RestController
@CrossOrigin(origins = "*")
public class ScheduleController {

	private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

	@Autowired
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
			@RequestParam("resume") MultipartFile resume,
			@RequestPart("schedule") String scheduleData,
			@RequestParam(required = false, name = "initialize") Boolean initialize) {
		
		ObjectMapper objectMapper = new ObjectMapper();
		Schedule schedule;
		try {
			schedule = objectMapper.readValue(scheduleData, Schedule.class);
			schedule =scheduleService.saveResumeFile(resume, schedule);
			
			if (initialize != null && initialize) {
				Set<String> emailSet = null;
				if (schedule.distributionList != null && !schedule.distributionList.isEmpty()) {
					emailSet = new HashSet<>();
					for (String email : schedule.distributionList) {
						emailSet.add(email.trim());
					}
				}
				scheduleService.scrapeAndEmailInitialJobs(schedule.prefs, emailSet);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Success(e.getMessage());
		}

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
	public ScheduledResults getResults(@PathVariable("candidateId") String candidateId,
			@RequestParam(required = false, name = "top") Integer top,
			@RequestParam(required = false, name = "minScore") Integer minScore) {
		ScheduledResults results = scheduleService.getResults(candidateId);
		if (results == null) {
			return null;
		}
		results.jobResult = Util.getTopJobs(results.jobResult, top, minScore);
		return results;
	}

	@GetMapping(value = "/scheduled/results/text/{candidateId}", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String getResultsText(@PathVariable("candidateId") String candidateId,
			@RequestParam(required = false, name = "top") Integer top,
			@RequestParam(required = false, name = "minScore") Integer minScore) {
		ScheduledResults results = getResults(candidateId, top, minScore);
		if (results == null) {
			return null;
		}
		return "Results as of: [" + results.dateTime + "]\n\n\n" + Util.formatJobs(results.jobResult);
	}
	
	

}
