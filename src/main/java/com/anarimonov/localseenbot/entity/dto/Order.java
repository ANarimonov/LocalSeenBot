package com.anarimonov.localseenbot.entity.dto;

import com.anarimonov.localseenbot.entity.Service;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Order {
    private int id;
    private Service service;
    private int quantity;
    private String link;
    private int answer;
}
