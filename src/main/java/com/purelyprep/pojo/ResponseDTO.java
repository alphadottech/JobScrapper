package com.purelyprep.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseDTO {

    private int statusCode;
    private String statusMessage;

}
