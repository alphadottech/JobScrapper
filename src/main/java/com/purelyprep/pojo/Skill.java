package com.purelyprep.pojo;

public class Skill {

    public String skill;
    public Integer yearsOfExperience;

    @Override
    public String toString() {
        return "Skill{" +
                "skill='" + skill + '\'' +
                ", yearsOfExperience=" + yearsOfExperience +
                '}';
    }
}
