package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.HrStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HrStaffRepository extends JpaRepository<HrStaff, Long> {
    List<HrStaff> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();
    List<HrStaff> findAllByOrderBySortOrderAscCreatedAtAsc();
    Optional<HrStaff> findByEmail(String email);
    boolean existsByEmail(String email);
}
