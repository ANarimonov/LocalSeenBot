package com.anarimonov.localseenbot.entity.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ResponseDto {
    private boolean success;
    private Data data;
}
