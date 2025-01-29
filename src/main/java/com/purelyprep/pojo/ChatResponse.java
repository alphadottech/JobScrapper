package com.purelyprep.pojo;

import java.io.Serializable;
import java.util.List;

public class ChatResponse implements Serializable {

    public ChatResponse() {}

    public List<Choice> choices;

    public static class Choice {

        public int index;
        public Message message;

    }

}
