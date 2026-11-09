package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_sessions")
public class HrSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String sectionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(length = 50)
    private String icon;

    @Column(length = 30)
    private String iconClass;

    private boolean solicitable = true;

    @Column(length = 200)
    private String requiredRoles;

    private int maxRecipients = 3;

    private boolean active = true;

    private int sortOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist

    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }
    public boolean isSolicitable() { return solicitable; }
    public void setSolicitable(boolean solicitable) { this.solicitable = solicitable; }
    public String getRequiredRoles() { return requiredRoles; }
    public void setRequiredRoles(String requiredRoles) { this.requiredRoles = requiredRoles; }
    public int getMaxRecipients() { return maxRecipients; }
    public void setMaxRecipients(int maxRecipients) { this.maxRecipients = maxRecipients; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
