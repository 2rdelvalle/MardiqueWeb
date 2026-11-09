package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.HrStaff;
import com.example.MardiqueWeb.Repository.HrStaffRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/hr-staff")
public class HrStaffController {

    private final HrStaffRepository repo;

    public HrStaffController(HrStaffRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> getActive() {
        return repo.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toMap).toList();
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAll() {
        return repo.findAllByOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String email = ((String) body.getOrDefault("email", "")).trim().toLowerCase();
        if (email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email requerido"));
        }
        if (repo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un colaborador con ese email"));
        }
        HrStaff s = new HrStaff();
        applyFields(s, body);
        s.setEmail(email);
        s = repo.save(s);
        return ResponseEntity.ok(toMap(s));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id)
                .map(s -> {
                    applyFields(s, body);
                    s = repo.save(s);
                    return ResponseEntity.ok((Object) toMap(s));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return repo.findById(id)
                .map(s -> {
                    s.setActive(false);
                    repo.save(s);
                    return ResponseEntity.ok((Object) Map.of("ok", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        return repo.findById(id)
                .map(s -> {
                    s.setActive(true);
                    repo.save(s);
                    return ResponseEntity.ok((Object) toMap(s));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void applyFields(HrStaff s, Map<String, Object> body) {
        if (body.containsKey("email")) s.setEmail(((String) body.get("email")).trim().toLowerCase());
        if (body.containsKey("name")) s.setName((String) body.get("name"));
        if (body.containsKey("role")) s.setRole((String) body.get("role"));
        if (body.containsKey("group")) s.setGroup((String) body.get("group"));
        if (body.containsKey("sortOrder")) s.setSortOrder(body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
        if (body.containsKey("active")) s.setActive(Boolean.TRUE.equals(body.get("active")));
    }

    private Map<String, Object> toMap(HrStaff s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("email", s.getEmail());
        m.put("name", s.getName());
        m.put("role", s.getRole());
        m.put("group", s.getGroup());
        m.put("active", s.isActive());
        m.put("sortOrder", s.getSortOrder());
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return m;
    }
}
