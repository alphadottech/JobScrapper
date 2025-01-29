package com.purelyprep.pojo;

import java.io.Serializable;
import java.util.List;

public class ScheduledResults implements Serializable {

    public ScheduledResults() {}

    public ScheduledResults(JobResult jobResult, String dateTime) {
        this.jobResult = jobResult;
        this.dateTime = dateTime;
    }

    public JobResult jobResult;
    public String dateTime;

}
