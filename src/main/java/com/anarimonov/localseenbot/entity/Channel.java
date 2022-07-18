package com.anarimonov.localseenbot.entity;

import com.anarimonov.localseenbot.entity.abs.AbsEntity;
import lombok.*;

import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = "channels")
public class Channel extends AbsEntity {
    private String channelId;
}
