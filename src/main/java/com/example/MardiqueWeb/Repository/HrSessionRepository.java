package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.HrSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HrSessionRepository extends JpaRepository<HrSession, Long> {
    Optional<HrSession> findBySectionId(String sectionId);
    List<HrSession> findAllByOrderBySortOrderAscCreatedAtAsc();
    List<HrSession> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();
    boolean existsBySectionId(String sectionId);
}
