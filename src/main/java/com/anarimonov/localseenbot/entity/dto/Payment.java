package com.anarimonov.localseenbot.entity.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Payment {
    private int amount;
    private double price;

}
