package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.DocumentDepartment;
import com.example.MardiqueWeb.Repository.DocumentDepartmentRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/departments")
public class DocumentDepartmentController {

    private final DocumentDepartmentRepository repo;

    public DocumentDepartmentController(DocumentDepartmentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<?> listAll() {
        List<DocumentDepartment> all = repo.findAllByOrderBySortOrderAsc();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/active")
    public ResponseEntity<?> listActive() {
        List<DocumentDepartment> active = repo.findByActiveTrueOrderBySortOrderAsc();
        return ResponseEntity.ok(active);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return repo.findById(id)
                .map(dept -> ResponseEntity.ok((Object) dept))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String slug = String.valueOf(body.getOrDefault("slug", "")).trim();
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        String icon = String.valueOf(body.getOrDefault("icon", "fa-folder")).trim();
        int sortOrder = body.containsKey("sortOrder") ? ((Number) body.get("sortOrder")).intValue() : 0;

        if (slug.isEmpty() || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "slug and name required"));
        }
        if (repo.existsBySlug(slug)) {
            return ResponseEntity.badRequest().body(Map.of("error", "slug already exists"));
        }

        DocumentDepartment dept = new DocumentDepartment();
        dept.setSlug(slug);
        dept.setName(name);
        dept.setIcon(icon.isEmpty() ? "fa-folder" : icon);
        dept.setSortOrder(sortOrder);
        dept.setActive(true);
        repo.save(dept);
        return ResponseEntity.ok(dept);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(dept -> {
            if (body.containsKey("name")) dept.setName(String.valueOf(body.get("name")).trim());
            if (body.containsKey("icon")) dept.setIcon(String.valueOf(body.get("icon")).trim());
            if (body.containsKey("sortOrder")) dept.setSortOrder(((Number) body.get("sortOrder")).intValue());
            if (body.containsKey("active")) dept.setActive((Boolean) body.get("active"));
            repo.save(dept);
            return ResponseEntity.ok((Object) dept);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
