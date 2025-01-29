package com.purelyprep.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Builder(toBuilder = true)
@AllArgsConstructor
public class CandidatePreferences implements Serializable {

    public CandidatePreferences() {}

    public CandidatePreferences(CandidatePreferences prefs) {
        this.candidateId = prefs.candidateId;
        this.candidateName=prefs.candidateName;
        this.jobTitles = prefs.jobTitles;
        this.jobLocations=prefs.jobLocations;
        this.jobTypes=prefs.jobTypes;
        this.physicalLocation=prefs.physicalLocation;
        //this.manager = prefs.manager;
        this.desiredLocations = prefs.desiredLocations;
        this.fullRemote = prefs.fullRemote;
        this.lowestSalary = prefs.lowestSalary;
        this.undesiredRoles = prefs.undesiredRoles;
        this.undesiredCompanies = prefs.undesiredCompanies;
        this.candidateResume = prefs.candidateResume;
        this.candidateResumePath = prefs.candidateResumePath;
        this.maxJobAgeDays = prefs.maxJobAgeDays;
        this.runId = prefs.runId;
        this.splitByCountry = prefs.splitByCountry;
        //this.seniorityLevel = prefs.seniorityLevel;
        this.desiredComapanies=prefs.desiredComapanies;
        this.desiredIndustry=prefs.desiredIndustry;
        this.undesiredIndustry=prefs.undesiredIndustry;
        this.desiredCompanySize=prefs.desiredCompanySize;
        this.undesiredCompanySize=prefs.undesiredCompanySize;
        this.reportSchedules=prefs.reportSchedules;
        this.undesiredLocations= prefs.undesiredLocations;
        this.minimumScore=prefs.minimumScore;
    }

    public String candidateId = "";
    public List<String> jobTitles = new ArrayList<>();
   // public boolean manager = false;
    public List<String> jobLocations= new ArrayList<>();
    public List<String> jobTypes= new ArrayList<>();
    public String physicalLocation= "";
    public List<String> desiredLocations = new ArrayList<>();
    public boolean fullRemote = false;
    public String lowestSalary = "";
    public List<String> undesiredRoles = new ArrayList<>();
    public Set<String> undesiredCompanies = new HashSet<>();
    public String candidateResume = "";
    public MultipartFile candidateResumeFile;
    public String candidateResumePath = "/home/ubuntu/Ajay_Resume.pdf";
    public Integer maxJobAgeDays = 14;
    public String runId = "";
    public Boolean splitByCountry = false;
    public List<DayOfWeek> reportSchedules= new ArrayList<>();
    //public String seniorityLevel = "";

    public String candidateName;
    public Set<String> desiredComapanies = new HashSet<>();
    public Set<String> desiredIndustry = new HashSet<>();
    public Set<String> undesiredIndustry = new HashSet<>();
    public String desiredCompanySize;
    public String undesiredCompanySize;
    public Set<String> undesiredLocations = new HashSet<>();
    public String minimumScore="";

    public static String getRunKey(String runId) {
        return "jobScraper-runId-" + runId;
    }

    public String getRunKey() {
        return getRunKey(this.runId);
    }

    public CandidatePreferences clone() {
        return new CandidatePreferences(this);
    }

    @Override
    public String toString() {
        return "CandidatePreferences{" +
                "candidateId='" + candidateId + '\'' +
                ", candidateName='" + candidateName + '\'' +
                ", jobTitle='" + jobTitles + '\'' +
                ", jobLocations='" + jobLocations + '\'' +
                ", jobTypes='" + jobTypes + '\'' +
                ", physicalLocation='" + physicalLocation + '\'' +
                //", manager=" + manager +
                ", desiredLocations=" + desiredLocations +
                ", fullRemote=" + fullRemote +
                ", lowestSalary='" + lowestSalary + '\'' +
                ", undesiredRoles=" + undesiredRoles +
                ", undesiredCompanies=" + undesiredCompanies +
                ", candidateResume='" + candidateResume + '\'' +
                ", candidateResumePath='" + candidateResumePath + '\'' +
                ", maxJobAgeDays=" + maxJobAgeDays +
                ", runId='" + runId + '\'' +
                ", splitByCountry=" + splitByCountry +
               // ", seniorityLevel='" + seniorityLevel + '\'' +
                ", desiredComapanies='" + desiredComapanies + '\'' +
                ", desiredIndustry='" + desiredIndustry + '\'' +
                ", undesiredIndustry='" + undesiredIndustry + '\'' +
                ", desiredCompanySize='" + desiredCompanySize + '\'' +
                ", undesiredLocations='" + undesiredLocations + '\'' +
                ", minimumScore='" + minimumScore + '\'' +
                ", undesiredCompanySize='" + undesiredCompanySize + '\'' +
                ", reportSchedules='" + reportSchedules + '\'' +
                '}';
    }
}