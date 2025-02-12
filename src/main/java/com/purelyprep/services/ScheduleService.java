package com.purelyprep.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;
import com.purelyprep.pojo.CandidatePreferences;
import com.purelyprep.pojo.Job;
import com.purelyprep.pojo.JobResult;
import com.purelyprep.pojo.Schedule;
import com.purelyprep.pojo.ScheduledResults;
import com.purelyprep.util.Util;

import jakarta.annotation.PostConstruct;

@Service
public class ScheduleService {

	private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
	private static final String setKey = RedisService.getSetKey(Schedule.class);
	public static final Set<String> defaultRecipients = new HashSet<>();
	public static JobResult excludedJobResult = new JobResult();

	private ObjectMapper objectMapper;
	static {
		defaultRecipients.add("vinodverma.adt@gmail.com");
		defaultRecipients.add("vverma.java@gmail.com");
	}

	private static ScheduleService scheduleService;

	public static ScheduleService getInstance() {
		return scheduleService;
	}

	
	  @Value("${resume.folder.path}") 
	  public String resumePath;
	  
	

	private static String getDailySetKey(DayOfWeek dayOfWeek) {
		return "scheduled-daily-set-" + setKey + "-" + dayOfWeek.getValue();
	}

	private static String getResultKey(String candidateId) {
		return "scheduled-results-" + candidateId;
	}

	private static String getScheduleKey(String candidateId) {
		return "scheduled-run-" + candidateId;
	}

	@PostConstruct
	private void init() {
		scheduleService = this;
	}

	private final RedisService redisService;
	private final EmailService emailService;
	private final RestTemplate restTemplate;

	public ScheduleService(RedisService redisService, EmailService emailService, RestTemplate restTemplate,
			ObjectMapper objectMapper) {
		this.redisService = redisService;
		this.emailService = emailService;
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

    @Scheduled(cron = "0 0 20 * * MON-FRI", zone = Util.timezone)
    public void runDailySchedule() {
        log.info("Running daily schedule");
        try {
			processDailySet(LocalDate.now(ZoneId.of(Util.timezone)).getDayOfWeek());
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	public List<Schedule> getSchedules(DayOfWeek dayOfWeek) {
		List<Schedule> schedules = new ArrayList<>();
		Set<String> candidates = redisService.members(getDailySetKey(dayOfWeek));
		if (candidates == null || candidates.isEmpty()) {
			return schedules;
		}
		for (String candidateId : candidates) {
			try {
				String scheduleString = redisService.get(getScheduleKey(candidateId));
//                Schedule schedule = redisService.get(getScheduleKey(candidateId));
				Schedule schedule = objectMapper.readValue(scheduleString, Schedule.class);
				if (schedule != null) {
					schedules.add(schedule);
				}
			} catch (Exception e) {
				log.error("Error fetching schedule: ", e);
			}
		}
		return schedules;
	}

	public List<Schedule> getAllSchedules() {
		List<Schedule> schedules = new ArrayList<>();
		for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
			schedules.addAll(getSchedules(dayOfWeek));
		}
		return schedules;
	}

	public Schedule saveResumeFile(MultipartFile resume,Schedule schedule) throws IOException {

		Object scheduleObject = redisService.get(getScheduleKey(schedule.prefs.candidateId));
		String existingResumePath = null;
		// Check if the object is a Map (could be LinkedHashMap)
		if (scheduleObject!=null&&scheduleObject instanceof Map) {
		     Schedule existingSchedule = objectMapper.convertValue(scheduleObject, Schedule.class);
		     existingResumePath=existingSchedule.prefs.candidateResumePath;
		}
		
		String originalFileName = resume.getOriginalFilename();
		String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
		String newFileName = System.currentTimeMillis() + "_resume" + fileExtension;

		Path destinationFolder = Paths.get(resumePath);

		if (!Files.exists(destinationFolder)) {
			Files.createDirectories(destinationFolder);
		}

	    Path filePath = destinationFolder.resolve(existingResumePath != null ? existingResumePath : newFileName);
	    File destFile = filePath.toFile();
	    
	    // If the file already exists, delete it before saving the new one
	    if (destFile.exists()) {
	        destFile.delete();
	    }
	    schedule.prefs.candidateResumePath=filePath.toString();
	    schedule.prefs.candidateResumeName=originalFileName;
	    if(schedule.prefs.jobLocations.size() == 1 &&schedule.prefs.jobLocations.contains("WFH")){
	    	schedule.prefs.fullRemote=true;
		}
	    // Transfer the file to the destination
	    resume.transferTo(destFile);
	    scheduleService.addToDailySchedule(schedule);
		return  schedule;
	}

	public void addToDailySchedule(Schedule schedule) {

		for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
			redisService.removeFromSet(getDailySetKey(dayOfWeek), schedule.prefs.candidateId);
		}
		addReportScheduleDays(schedule);
	}

	private void addReportScheduleDays(Schedule schedule) {
		for (DayOfWeek dayOfWeeks : schedule.prefs.reportSchedules) {
			redisService.addToSet(getDailySetKey(dayOfWeeks), schedule.prefs.candidateId);
			redisService.set(getScheduleKey(schedule.prefs.candidateId), schedule, 15, TimeUnit.DAYS);
		}
	}

	/*
	 * public void addToDailySchedule(Schedule schedule) { for (DayOfWeek dayOfWeek
	 * : DayOfWeek.values()) { redisService.removeFromSet(getDailySetKey(dayOfWeek),
	 * schedule.prefs.candidateId); }
	 * redisService.addToSet(getDailySetKey(schedule.dayOfWeek),
	 * schedule.prefs.candidateId);
	 * redisService.set(getScheduleKey(schedule.prefs.candidateId), schedule, 15,
	 * TimeUnit.DAYS); }
	 */

	public ScheduledResults getResults(String candidateId) {
		return redisService.get(getResultKey(candidateId));
	}

	public void removeFromSchedule(Schedule schedule) {
		for (DayOfWeek dayOfWeeks : schedule.prefs.reportSchedules) {
			redisService.removeFromSet(getDailySetKey(dayOfWeeks), schedule.prefs.candidateId);
			redisService.delete(getScheduleKey(schedule.prefs.candidateId));
		}
	}

	@Async
	public void scrapeAndEmailInitialJobs(CandidatePreferences prefs, Set<String> emails) {
		removeLastSent(prefs.candidateId);
		JobScraper jobScraper = new JobScraper(JobScraper.MAX_JOBS, restTemplate);
		
		JobResult jobResult = jobScraper.getJobsForCandidate(prefs, true);
		emails = (emails != null && !emails.isEmpty() ? emails : defaultRecipients);
		if(jobResult!=null)
		redisService.set(getResultKey(prefs.candidateId), jobResult, 8, TimeUnit.DAYS);
		emailService.sendPlainText(EmailService.from, emails, getEmailSubject(prefs.candidateId, true),
				getEmailBody(jobResult, true));
		if(ScheduleService.excludedJobResult!=null) {
			emailService.sendPlainText(EmailService.from, emails, getEmailSubject(prefs.candidateId, true),
					getEmailBody(ScheduleService.excludedJobResult, true));
		}
	}

	public LocalDateTime getLastSent(String candidateId) {
		return redisService.get(getLastSentKey(candidateId));
	}

	public void setLastSent(String candidateId) {
		redisService.set(getLastSentKey(candidateId), LocalDateTime.now(ZoneId.of(Util.timezone)), 8, TimeUnit.DAYS);
	}

	public void removeLastSent(String candidateId) {
		redisService.delete(getLastSentKey(candidateId));
	}

	private void processDailySet(DayOfWeek dayOfWeek) throws JsonMappingException, JsonProcessingException {
		Set<String> candidates = redisService.members(getDailySetKey(dayOfWeek));
		if (candidates == null || candidates.isEmpty()) {
			return;
		}

		log.info("Candidates: [" + candidates + "]");
		for (String candidateId : candidates) {
			Object scheduleObj = redisService.get(getScheduleKey(candidateId));
			Schedule schedule=null;
			if (scheduleObj!=null&&scheduleObj instanceof Map) {
				schedule = objectMapper.convertValue(scheduleObj, Schedule.class);
			}
//            String  scheduleString = redisService.get(getScheduleKey(candidateId));
//           Schedule schedule= objectMapper.readValue(scheduleString, Schedule.class);
			log.info("Popped Schedule for Candidate: [" + schedule + "]");
			if (schedule != null) {
//				LocalDateTime lastSent = getLastSent(candidateId);
//				if (lastSent != null
//						&& Duration.between(lastSent, LocalDateTime.now(ZoneId.of(Util.timezone))).toDays() <= 5) {
//					log.info("Sent <= 5 days ago: ["
//							+ lastSent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
//					continue;
//				}
				JobScraper jobScraper = new JobScraper(JobScraper.MAX_JOBS, restTemplate);
				JobResult jobResult = jobScraper.getJobsForCandidate(schedule.prefs, true);
				ScheduledResults results = new ScheduledResults(jobResult, Util.getNow());
				redisService.set(getResultKey(candidateId), results, 8, TimeUnit.DAYS);
				emailService.sendPlainText(EmailService.from, defaultRecipients, getEmailSubject(candidateId, false),
						getEmailBody(jobResult, false));
			}
		}
	}

	private String getEmailBody(JobResult jobResult, boolean initial) {
		return (initial ? "Initial " : "") + "Results as of: [" + Util.getNow() + "]\n\n\n"
				+ Util.formatTopJobs(jobResult, 100, JobScraper.MIN_SCORE);
	}

	private String getEmailSubject(String candidateId, boolean initial) {
		return (initial ? "Initial " : "") + "Job Results for: [" + candidateId + "]";
	}

	private String getLastSentKey(String candidateId) {
		return "last-sent-" + candidateId;
	}
}
