package com.anarimonov.localseenbot.repository;

import com.anarimonov.localseenbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
    Integer countByReferrerId(Long referrerId);
}
