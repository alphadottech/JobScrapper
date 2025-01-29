package com.purelyprep.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.purelyprep.pojo.*;
import com.purelyprep.util.Util;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private static final String setKey = RedisService.getSetKey(Schedule.class);
    public static final Set<String> defaultRecipients = new HashSet<>();
    private ObjectMapper objectMapper;
    static {
        defaultRecipients.add("vinodverma.adt@gmail.com");
        defaultRecipients.add("vverma.java@gmail.com");
    }

    private static ScheduleService scheduleService;

    public static ScheduleService getInstance() {
        return scheduleService;
    }


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

    public ScheduleService(RedisService redisService, EmailService emailService, RestTemplate restTemplate,ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
        this.objectMapper=objectMapper;
    }

//    @Scheduled(cron = "* 1 * * * MON-FRI", zone = Util.timezone)
//    public void runDailySchedule() {
//        log.info("Running daily schedule");
//        try {
//			processDailySet(LocalDate.now(ZoneId.of(Util.timezone)).getDayOfWeek());
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    }

    public List<Schedule> getSchedules(DayOfWeek dayOfWeek) {
        List<Schedule> schedules = new ArrayList<>();
        Set<String> candidates = redisService.members(getDailySetKey(dayOfWeek));
        if (candidates == null || candidates.isEmpty()) {
            return schedules;
        }
        for (String candidateId : candidates) {
            try {
                String  scheduleString = redisService.get(getScheduleKey(candidateId));
//                Schedule schedule = redisService.get(getScheduleKey(candidateId));
                Schedule schedule= objectMapper.readValue(scheduleString, Schedule.class);
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

    public void addToDailySchedule(Schedule schedule){
        for (DayOfWeek dayOfWeek : DayOfWeek.values()){
            redisService.removeFromSet(getDailySetKey(dayOfWeek), schedule.prefs.candidateId);
        }
        addReportScheduleDays(schedule);
    }

    private void addReportScheduleDays(Schedule schedule) {
        for(DayOfWeek dayOfWeeks : schedule.prefs.reportSchedules){
            redisService.addToSet(getDailySetKey(dayOfWeeks), schedule.prefs.candidateId);
            redisService.set(getScheduleKey(schedule.prefs.candidateId), schedule, 15, TimeUnit.DAYS);
        }
    }

   /* public void addToDailySchedule(Schedule schedule) {
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            redisService.removeFromSet(getDailySetKey(dayOfWeek), schedule.prefs.candidateId);
        }
        redisService.addToSet(getDailySetKey(schedule.dayOfWeek), schedule.prefs.candidateId);
        redisService.set(getScheduleKey(schedule.prefs.candidateId), schedule, 15, TimeUnit.DAYS);
    }*/

    public ScheduledResults getResults(String candidateId) {
        return redisService.get(getResultKey(candidateId));
    }

    public void removeFromSchedule(Schedule schedule) {
        for(DayOfWeek dayOfWeeks : schedule.prefs.reportSchedules) {
            redisService.removeFromSet(getDailySetKey(dayOfWeeks), schedule.prefs.candidateId);
            redisService.delete(getScheduleKey(schedule.prefs.candidateId));
        }
    }

    @Async
    public void scrapeAndEmailInitialJobs(CandidatePreferences prefs, Set<String> emails, Boolean setSent) {
        setSent = setSent == null || setSent;
        removeLastSent(prefs.candidateId);
        JobScraper jobScraper = new JobScraper(JobScraper.MAX_JOBS, restTemplate);
        JobResult jobResult = jobScraper.getJobsForCandidate(prefs, setSent);
        emails = (emails != null && !emails.isEmpty() ? emails : defaultRecipients);
        emailService.sendPlainText(
            EmailService.from,
            emails,
            getEmailSubject(prefs.candidateId, true),
            getEmailBody(jobResult, true)
        );
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
            Schedule schedule = redisService.get(getScheduleKey(candidateId));
//            String  scheduleString = redisService.get(getScheduleKey(candidateId));
//           Schedule schedule= objectMapper.readValue(scheduleString, Schedule.class);
            log.info("Popped Schedule for Candidate: [" + schedule + "]");
            if (schedule != null) {
                LocalDateTime lastSent = getLastSent(candidateId);
                if (lastSent != null && Duration.between(lastSent, LocalDateTime.now(ZoneId.of(Util.timezone))).toDays() <= 5) {
                    log.info("Sent <= 5 days ago: [" + lastSent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
                    continue;
                }
                JobScraper jobScraper = new JobScraper(JobScraper.MAX_JOBS, restTemplate);
                JobResult jobResult = jobScraper.getJobsForCandidate(schedule.prefs, true);
                ScheduledResults results = new ScheduledResults(jobResult, Util.getNow());
                redisService.set(getResultKey(candidateId), results, 8, TimeUnit.DAYS);
                emailService.sendPlainText(
                    EmailService.from,
                    defaultRecipients,
                    getEmailSubject(candidateId, false),
                    getEmailBody(jobResult, false)
                );
            }
        }
    }

    private String getEmailBody(JobResult jobResult, boolean initial) {
        return (initial ? "Initial " : "") +
                "Results as of: [" + Util.getNow() + "]\n\n\n" + Util.formatTopJobs(jobResult, 100, JobScraper.MIN_SCORE);
    }

    private String getEmailSubject(String candidateId, boolean initial) {
        return (initial ? "Initial " : "") + "Job Results for: [" + candidateId + "]";
    }

    private String getLastSentKey(String candidateId) {
        return "last-sent-" + candidateId;
    }
}
