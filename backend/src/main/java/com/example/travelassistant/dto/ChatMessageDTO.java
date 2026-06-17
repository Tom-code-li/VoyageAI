package com.example.travelassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {

    private Long id;

    private String sender;

    private String content;

    private LocalDateTime createTime;
}
