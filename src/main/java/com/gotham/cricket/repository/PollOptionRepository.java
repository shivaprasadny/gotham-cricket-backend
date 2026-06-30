package com.gotham.cricket.repository;

import com.gotham.cricket.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollIdOrderByDisplayOrderAscIdAsc(Long pollId);
}
