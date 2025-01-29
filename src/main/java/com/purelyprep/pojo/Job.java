package com.purelyprep.pojo;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.Serializable;
import java.util.List;

public class Job implements Serializable {
    public String jobId;
    public String title;
    public String companyName;
    public String url;
    public String salary;
    public String descr;
    public double score;
    public String explanation;
    public List<String> locations;
    public String seniorityLevel;
    public String runDate;

    public static String getJobUuid(String companyName, String title) {
        return DigestUtils.md5Hex(companyName + "|" + title).toUpperCase();
    }

    public static int seniorityLevelToInt(String seniorityLevel) {
        if (seniorityLevel == null || seniorityLevel.isEmpty()) {
            return -1;
        }
        switch (seniorityLevel.toLowerCase()) {
            case "internship": return 0;
            case "entry level": return 1;
            case "associate": return 2;
            case "mid-senior level": return 3;
            case "director": return 4;
            case "executive": return 5;
            default: return -1;
        }
    }

    public static String inToSeniorityLevel(int seniorityLevel) {
        if (seniorityLevel < 0) {
            return "";
        }
        switch (seniorityLevel) {
            case 0: return "internship";
            case 1: return "entry level";
            case 2: return "associate";
            case 3: return "mid-senior level";
            case 4: return "director";
            case 5: return "executive";
            default: return "";
        }
    }

    public Job(){}

    public Job(String jobId, String title, String companyName, String url, String salary, String descr, double score, String explanation,
               List<String> locations, String runDate) {
        this.jobId = jobId;
        this.title = title;
        this.companyName = companyName;
        this.url = url;
        this.salary = salary;
        this.descr = descr;
        this.score = score;
        this.explanation = explanation;
        this.locations = locations;
        //this.seniorityLevel = seniorityLevel;
        this.runDate = runDate;
    }

    public String getJobUuid() {
        return getJobUuid(this.companyName, this.title);
    }

    @Override
    public String toString() {
        return "Job{" +
                "jobId='" + jobId + '\'' +
                ", title='" + title + '\'' +
                ", companyName='" + companyName + '\'' +
                ", url='" + url + '\'' +
                ", salary='" + salary + '\'' +
                ", descr='" + descr + '\'' +
                ", score=" + score +
                ", explanation='" + explanation + '\'' +
                ", locations=" + locations +
               // ", seniorityLevel='" + seniorityLevel + '\'' +
                ", runDate='" + runDate + '\'' +
                '}';
    }
}