package com.purelyprep.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.purelyprep.pojo.Job;
import com.purelyprep.pojo.JobResult;
import com.purelyprep.pojo.LatLong;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Util {

    private static final Logger log = LoggerFactory.getLogger(Util.class);
    public static final ObjectMapper objectMapper = new ObjectMapper();
    private static final double EARTH_RADIUS = 3958.8; // in miles
//    public static final String timezone = "America/Los_Angeles";
    public static final String timezone = "Asia/Kolkata";
    public static void waitRandom() {
        waitRandom(20, 300);
    }

    public static void waitRandom(int minMs, int maxMs) {
        int ms = ThreadLocalRandom.current().nextInt(minMs, maxMs + 1);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error("Error waiting: ", e);
        }
    }

    public static double distance(LatLong desired, LatLong loc) {

        double latDistance = Math.toRadians(loc.lat - desired.lat);
        double lonDistance = Math.toRadians(loc.lng - desired.lng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(desired.lat)) * Math.cos(Math.toRadians(loc.lat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    	}



    public static String encodeFileToBase64(String fileName) {
        try {
            File file = new File(fileName);

             byte[] encoded = Base64.getEncoder().encode(FileUtils.readFileToByteArray(file));
            return new String(encoded, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            log.error("Error encoding file: ", e);
            return null;
        }
    }

    public static File decodeBase64ToFile(String filename, String base64) {
        try {
            File dir = Files.createTempDirectory("temp-dir").toFile();
            dir.deleteOnExit();
            File file = new File(dir, filename);
            file.deleteOnExit();
            FileUtils.writeByteArrayToFile(file, Base64.getDecoder().decode(base64));
            return file;
        } catch (Exception e) {
            log.error("Error decoding to file: ", e);
            return null;
        }
    }

    public static Map<String, Object> parseMap(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr.replaceAll("\\s", "").equals("{}")) {
            return new HashMap<>();
        }
        jsonStr = jsonStr.replace("```json", "").replace("```", "").trim();
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<>() {});
        } catch (Exception e) {
            log.info("Failed to parse map: [" + jsonStr + "]");
        }
        return null;
    }

    public static List<String> parseList(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty() || jsonStr.replaceAll("\\s", "").equals("[]")) {
            return new ArrayList<>();
        }
        jsonStr = jsonStr.replace("```json", "").replace("```", "").trim();
        try {
            return objectMapper.readValue(jsonStr, TypeFactory.defaultInstance().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.info("Failed to parse list: [" + jsonStr + "]");
        }
        return null;
    }

    public static String getNow() {
        return LocalDateTime.now(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String formatJobs(JobResult jobResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Processed [" + jobResult.totalJobsProcessed + "] Jobs for Titles: [" +
                jobResult.titles.stream().collect(Collectors.joining(", ")) + "]\n\n\n");
        for (Job job : jobResult.jobs) {
            sb.append("Title: " + job.title + "\n");
            sb.append("Salary: " + job.salary + "\n");
            sb.append("Locations: [ " + String.join(", ", job.locations) + " ]\n");
            sb.append("Score: " + job.score + "\n");
            sb.append("Link: " + job.url + "\n");
            sb.append("Explanation: " + job.explanation);
            sb.append("\n\n\n");
        }
        return sb.toString();
    }

    public static String formatExcludedJobs(JobResult jobResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Processed [" + jobResult.totalJobsProcessed + "] Jobs for Titles: [" +
                jobResult.titles.stream().collect(Collectors.joining(", ")) + "]\n\n\n");
        for (Job job : jobResult.jobs) {
            sb.append("Title: " + job.title + "\n");
            sb.append("Link: " + job.url + "\n");
            sb.append("Explanation: " + job.explanation);
            sb.append("\n\n\n");
        }
        return sb.toString();
    }
    
    public static String formatTopJobs(JobResult jobResult, Integer top, Integer minScore) {
        return Util.formatJobs(Util.getTopJobs(jobResult, top, minScore));
    }

    public static JobResult getTopJobs(JobResult jobResult, Integer top, Integer minScore) {
        final double minScoreFinal = (minScore == null ? 0 : minScore);
        jobResult.jobs.sort((job1, job2) -> (int) Math.signum(job2.score - job1.score));
        if (top == null || top <= 0 || top > jobResult.jobs.size()) {
            if (minScoreFinal <= 0) {
                return jobResult;
            }
            jobResult.jobs = jobResult.jobs.stream().filter(j -> j.score >= minScoreFinal).collect(Collectors.toList());
            return jobResult;
        }
        jobResult.jobs = jobResult.jobs.stream().limit(top).filter(j -> j.score >= minScoreFinal).collect(Collectors.toList());
        return jobResult;
    }

}
