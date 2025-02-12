package com.purelyprep.pojo;
public enum LocationKeyword {
    GREATER("Greater"),
    AREA("Area"),
    REGION("Region"),
    BAY("Bay"),
    METROPOLITAN("Metropolitan"),
    METROPLEX("Metroplex");

    private final String keyword;

    LocationKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    // Method to remove all keywords from the location string
    public static String removeKeywords(String location) {
        if (location != null) {

            if (location.contains("-")) {
                location = location.split("-")[0];
            }

            for (LocationKeyword keyword : LocationKeyword.values()) {
                location = location.replace(keyword.getKeyword(), "");
            }
        }
        return location;
    }
}
