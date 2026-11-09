package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.HrSession;
import com.example.MardiqueWeb.Repository.HrSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/hr-sessions")
public class HrSessionController {

    private final HrSessionRepository repo;

    public HrSessionController(HrSessionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return repo.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toMap).toList();
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getAllIncludingInactive() {
        return repo.findAllByOrderBySortOrderAscCreatedAtAsc().stream()
                .map(this::toMap).toList();
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<?> getBySectionId(@PathVariable String sectionId) {
        return repo.findBySectionId(sectionId)
                .map(s -> ResponseEntity.ok((Object) toMap(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String sectionId = (String) body.getOrDefault("sectionId", "");
        if (sectionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sectionId requerido"));
        }
        if (repo.existsBySectionId(sectionId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una sesión con ese ID"));
        }
        HrSession s = new HrSession();
        applyFields(s, body);
        s = repo.save(s);
        return ResponseEntity.ok(toMap(s));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<?> update(@PathVariable String sectionId, @RequestBody Map<String, Object> body) {
        return repo.findBySectionId(sectionId)
                .map(s -> {
                    applyFields(s, body);
                    s = repo.save(s);
                    return ResponseEntity.ok((Object) toMap(s));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<?> delete(@PathVariable String sectionId) {
        return repo.findBySectionId(sectionId)
                .map(s -> {
                    s.setActive(false);
                    repo.save(s);
                    return ResponseEntity.ok((Object) Map.of("ok", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void applyFields(HrSession s, Map<String, Object> body) {
        if (body.containsKey("sectionId")) s.setSectionId((String) body.get("sectionId"));
        if (body.containsKey("title")) s.setTitle((String) body.get("title"));
        if (body.containsKey("description")) s.setDescription((String) body.get("description"));
        if (body.containsKey("instructions")) s.setInstructions((String) body.get("instructions"));
        if (body.containsKey("icon")) s.setIcon((String) body.get("icon"));
        if (body.containsKey("iconClass")) s.setIconClass((String) body.get("iconClass"));
        if (body.containsKey("solicitable")) s.setSolicitable(Boolean.TRUE.equals(body.get("solicitable")));
        if (body.containsKey("requiredRoles")) s.setRequiredRoles((String) body.get("requiredRoles"));
        if (body.containsKey("maxRecipients")) s.setMaxRecipients(body.get("maxRecipients") instanceof Number n ? n.intValue() : 3);
        if (body.containsKey("sortOrder")) s.setSortOrder(body.get("sortOrder") instanceof Number n ? n.intValue() : 0);
        if (body.containsKey("active")) s.setActive(Boolean.TRUE.equals(body.get("active")));
    }

    private Map<String, Object> toMap(HrSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("sectionId", s.getSectionId());
        m.put("title", s.getTitle());
        m.put("description", s.getDescription());
        m.put("instructions", s.getInstructions());
        m.put("icon", s.getIcon());
        m.put("iconClass", s.getIconClass());
        m.put("solicitable", s.isSolicitable());
        m.put("requiredRoles", s.getRequiredRoles());
        m.put("maxRecipients", s.getMaxRecipients());
        m.put("active", s.isActive());
        m.put("sortOrder", s.getSortOrder());
        m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        return m;
    }
}
