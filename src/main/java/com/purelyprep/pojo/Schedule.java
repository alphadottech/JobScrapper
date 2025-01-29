package com.purelyprep.pojo;

import jdk.jfr.DataAmount;
import lombok.*;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Schedule implements Serializable {

    public CandidatePreferences prefs;
   // public DayOfWeek dayOfWeek;
    public List<String> distributionList= new ArrayList<>();

    @Override
    public String toString() {
        return "Schedule{" +
                "prefs=" + prefs +
            //    ", dayOfWeek=" + dayOfWeek +
                ", distributionList=" + distributionList +
                '}';
    }
}
