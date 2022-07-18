package com.anarimonov.localseenbot.repository;

import com.anarimonov.localseenbot.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivity, Integer> {
    UserActivity findByUserId(Long id);
}
