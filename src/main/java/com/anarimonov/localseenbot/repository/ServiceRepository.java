package com.anarimonov.localseenbot.repository;

import com.anarimonov.localseenbot.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    List<Service> findByCategory(String category);
}
