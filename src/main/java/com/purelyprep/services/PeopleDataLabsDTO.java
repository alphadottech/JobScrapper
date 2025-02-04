package com.purelyprep.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PeopleDataLabsDTO {
    @JsonProperty("data")
    private List<AutocompleteResult> data;

    @JsonProperty("fields")
    private List<String> fields;

    @JsonProperty("status")
    private int status;

    public List<AutocompleteResult> getData() {
        return data;
    }

    public void setData(List<AutocompleteResult> data) {
        this.data = data;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public static class AutocompleteResult {
        @JsonProperty("name")
        private String name;

        @JsonProperty("count")
        private int count;

        @JsonProperty("meta")
        private Object meta;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public Object getMeta() {
            return meta;
        }

        public void setMeta(Object meta) {
            this.meta = meta;
        }
    }
}