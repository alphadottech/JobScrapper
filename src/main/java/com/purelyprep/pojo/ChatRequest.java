package com.purelyprep.pojo;

import java.io.Serializable;
import java.util.List;

public class ChatRequest implements Serializable {

    public ChatRequest() {}

    public ChatRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String model;
    public List<Message> messages;
    public Double temperature = 0.6;

}
