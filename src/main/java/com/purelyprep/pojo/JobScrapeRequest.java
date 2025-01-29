package com.purelyprep.pojo;

import java.io.Serializable;

public class JobScrapeRequest implements Serializable {

    public JobScrapeRequest() {}

    public JobScrapeRequest(CandidatePreferences prefs, boolean setSent) {
        this.prefs = prefs;
        this.setSent = setSent;
    }

    public CandidatePreferences prefs;
    public boolean setSent;

}
