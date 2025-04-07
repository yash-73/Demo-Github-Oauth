package com.github.oauth.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Long id;
    private String message;
    private Long userId;
    private Long projectId;
    private LocalDateTime timeStamp;

    public Message(String message, Long projectId){
        this.message = message;
        this.projectId = projectId;
    }
}
