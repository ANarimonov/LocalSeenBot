package com.anarimonov.localseenbot.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity(name = "users")
public class User {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    @ManyToOne
    private User referrer;
}
