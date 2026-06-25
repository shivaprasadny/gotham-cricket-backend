package com.gotham.cricket.repository;

import com.gotham.cricket.entity.AnonymousReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnonymousReportRepository extends JpaRepository<AnonymousReport, Long> {
    List<AnonymousReport> findByMessageId(Long messageId);
    List<AnonymousReport> findByRoomId(Long roomId);
    boolean existsByMessageIdAndReporterId(Long messageId, Long reporterId);
}
