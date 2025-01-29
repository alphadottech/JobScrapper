package com.purelyprep.pojo;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

public class JobDetails implements Serializable {

    public JobDetails() {}

    public JobDetails(String salary, String jobDescr, Map<String, String> itemDict) {
        this.salary = salary;
        this.jobDescr = jobDescr;
        this.itemDict = itemDict;
    }

    public String salary;
    public String jobDescr;
    public Map<String, String> itemDict;

    @Override
    public String toString() {
        return "JobDetails{" +
                "salary='" + salary + '\'' +
                ", jobDescr='" + jobDescr + '\'' +
                ", itemDict=[{" + itemDict.entrySet().stream().map(entry ->
                    "key=" + entry.getKey() + ", value=" + entry.getValue()).collect(Collectors.joining("}, {")) + "}]" +
                '}';
    }
}
