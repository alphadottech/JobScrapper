package com.purelyprep.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JobResult implements Serializable {

    public JobResult() {}

    public JobResult(List<Job> jobs, int totalJobsProcessed, Set<String> titles) {
        this.jobs = jobs;
        this.totalJobsProcessed = totalJobsProcessed;
        this.titles = titles;
    }

    public List<Job> jobs = new ArrayList<>();
    public int totalJobsProcessed = 0;
    public Set<String> titles = new HashSet<>();

    public void merge(JobResult jobResult) {
        this.jobs.addAll(jobResult.jobs);
        this.totalJobsProcessed += jobResult.totalJobsProcessed;
        this.titles.addAll(jobResult.titles);
    }

    @Override
    public String toString() {
        return "JobResult{" +
                "jobs=" + jobs +
                ", totalJobsProcessed=" + totalJobsProcessed +
                ", titles=" + titles +
                '}';
    }
}
