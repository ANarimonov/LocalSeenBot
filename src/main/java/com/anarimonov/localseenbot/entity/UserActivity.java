package com.anarimonov.localseenbot.entity;

import com.anarimonov.localseenbot.entity.abs.AbsEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Entity(name = "usersActivities")
public class UserActivity extends AbsEntity {
    @OneToOne
    private User user;
    private String languageCode;
    private String role;
    private int step;
    private int coins;
    private boolean isStarted;

    public UserActivity setStep(int step) {
        this.step = step;
        return this;
    }

    public UserActivity setCoins(int coins) {
        this.coins = coins;
        return this;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
