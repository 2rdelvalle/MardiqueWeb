package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.DocumentDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentDepartmentRepository extends JpaRepository<DocumentDepartment, Long> {
    Optional<DocumentDepartment> findBySlug(String slug);
    List<DocumentDepartment> findAllByOrderBySortOrderAsc();
    List<DocumentDepartment> findByActiveTrueOrderBySortOrderAsc();
    boolean existsBySlug(String slug);
}
