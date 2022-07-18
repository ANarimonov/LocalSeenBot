package com.anarimonov.localseenbot.repository;

import com.anarimonov.localseenbot.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel,Integer> {
    Channel findByChannelId(String text);
}
