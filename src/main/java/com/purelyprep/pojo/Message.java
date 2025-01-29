package com.purelyprep.pojo;

import java.io.Serializable;

public class Message implements Serializable {

    public Message() {}

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public Role role;
    public String content;

}