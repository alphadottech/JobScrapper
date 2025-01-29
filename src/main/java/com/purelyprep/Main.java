//package com.purelyprep;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.purelyprep.pojo.ChatRequest;
//import com.purelyprep.pojo.Message;
//import com.purelyprep.pojo.Role;
//
//import java.util.ArrayList;
//
//public class Main {
//
//    public static void main(String[] args) {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        ChatRequest chatRequest = new ChatRequest();
//        chatRequest.model = "model";
//        chatRequest.messages = new ArrayList<>();
//        chatRequest.messages.add(new Message(Role.system, "content"));
//        chatRequest.messages.add(new Message(Role.user, "content2"));
//
//        try {
//            System.out.println(objectMapper.writeValueAsString(chatRequest));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//}
