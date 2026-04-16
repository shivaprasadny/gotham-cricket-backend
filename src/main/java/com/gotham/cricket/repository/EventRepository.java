package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Newest upcoming first
    List<Event> findAllByOrderByEventDateAsc();
}