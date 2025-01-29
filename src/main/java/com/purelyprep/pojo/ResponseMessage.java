package com.purelyprep.pojo;

import java.io.Serializable;

public class ResponseMessage<T> implements Serializable {

    public ResponseMessage() {}

    public ResponseMessage(String message) {
        this(message, message);
    }

    public ResponseMessage(String message, String messageCode) {
        this.message = message;
        this.messageCode = messageCode;
    }

    public ResponseMessage(String message, String messageCode, T response) {
        this.message = message;
        this.messageCode = messageCode;
        this.response = response;
    }

    public ResponseMessage(T response) {
        this.message = "success";
        this.messageCode = "success";
        this.response = response;
    }

    public String message;
    public String messageCode;
    public T response;

}
