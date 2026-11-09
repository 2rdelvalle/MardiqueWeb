---
name: memory-optimization
triggers: ["continuar", "última sesión", "contexto", "anterior", "siguiente", "retomar", "seguir"]
description: Gestionar memoria, caché y contexto para maximizar eficiencia en sesiones. Activar cuando la sesión dure > 30 min o el usuario pida continuar desde una sesión anterior.
---

# Memory Optimization Skill

## Propósito
Gestionar memoria, caché y contexto para maximizar eficiencia en sesiones de Free tier con límites dinámicos.

## Problema en Free Tier
- Límites de peticiones varían según carga de red
- Contexto acumulado causa timeouts
- Re-lectura de archivos desperdicia tokens
- Sesiones largas degradan rendimiento

## Solución: Memory Stack

### 1. Inicialización de Sesión

**Al comenzar una sesión:**
```markdown
## Session Memory - [DATE HH:MM]

**Project:** Sociedad-Portuaria-Mardique
**Branch:** feature/ai-skills-optimization
**Context Slots:** 5/10 (máximo)

### Loaded Contexts
1. Project architecture (/ai-skills/README.md) - 2KB
2. Distilled notes (last-session.md) - 1KB
3. Current task scope - 0.5KB
4. File references (caching) - 1KB
5. Dependencies map - 0.5KB

**Total:** 5KB / 128KB budget
**Remaining:** 123KB available
```

### 2. Context Budget Allocation

```
Total Budget: 128KB (aproximado en Free tier)
├── Permanent Contexts (always loaded): 10KB
│   ├── Project architecture
│   ├── Skills index
│   └── Quick reference
├── Task Context (current work): 40KB
│   ├── Current files being edited
│   ├── Related dependencies
│   └── Error logs
├── Cache Layer (frequently accessed): 30KB
│   ├── Recent file reads
│   ├── API responses
│   └── Build outputs
└── Buffer (safety margin): 48KB
    └── Reserve para impredecibles
```

### 3. Memory Profiling

**Monitorear uso por sesión:**
```markdown
## Memory Audit - 2026-09-03

| Context | Size | Usage % | Status |
|---------|------|---------|--------|
| code-distillation.md | 2.3KB | 2% | ✅ Keep |
| chunk-reading.md | 4.4KB | 3% | ✅ Keep |
| AdminController.java | 15KB | 12% | ⚠️ Chunk only |
| UserService.java | 18KB | 14% | ⚠️ Chunk only |
| IntranetDocumentos.html | 90KB | 70% | ❌ Too large |

**Action Items:**
- [ ] Split IntranetDocumentos.html en 3 chunks
- [ ] Archive old build.log
- [ ] Clear cache de commits antiguos
```

### 4. Caching Strategy

**Qué cachear (reutilizable entre sesiones):**
```
.project-memory/cache/
├── file-index.json          # Mapa de archivos + tamaños
├── dependency-graph.json    # Relaciones entre clases
├── api-endpoints.json       # Rutas del proyecto
├── entity-schema.json       # Estructura JPA
└── last-reads.json          # Últimas lecturas para hot-reload
```

**Ejemplo:**
```json
{
  "files": {
    "UserService.java": {
      "path": "src/main/java/.../UserService.java",
      "size": "18KB",
      "chunks": [
        {"method": "getUserById", "lines": "61-85"},
        {"method": "createUser", "lines": "86-150"},
        {"method": "updateUser", "lines": "151-220"}
      ],
      "last_modified": "2026-09-03T14:20:00Z",
      "hash": "abc123def456"
    }
  }
}
```

### 5. Session Lifecycle

```
[START SESSION]
    ↓
[1] Load permanent contexts (5KB)
    ↓
[2] Read distilled-notes.md (1KB)
    ↓
[3] Load file-index cache (2KB)
    ↓
[4] Ask user: "What's your task?"
    ↓
[5] Load task-specific context (20-30KB)
    ↓
[6] Work on task (read/write chunks)
    ↓
[7] Before exit: Distill changes
    ↓
[8] Update cache files
    ↓
[END SESSION] - Context cleared, cache persists
```

### 6. Cleanup Triggers

**Limpiar memoria cuando:**
- Archivo leído hace > 2 horas
- Tamaño total > 100KB
- Más de 10 archivos cargados
- Sesión activa > 45 minutos

**Acción:**
```bash
# Generar distilled-notes.md
# Guardar en cache/ lo que sea reutilizable
# Descargar todo lo demás del contexto
```

### 7. Indicators de Memory Health

```markdown
## Memory Health Check

✅ GREEN (< 50KB used)
- Sesión fluida
- Sin throttling
- Continuar normalmente

⚠️ YELLOW (50-90KB used)
- Empezar a cachear
- Limpiar files antiguos
- Reducir chunks leídos

🔴 RED (> 90KB used)
- Distill inmediatamente
- Terminar sesión
- Reiniciar nueva sesión con contexto limpio
```

### 8. Quick Reference Permanente

Mantén SIEMPRE cargado:
```markdown
## Quick Ref - Mardique Project

**Key Paths:**
- Controllers: src/main/java/.../Controller/
- Services: src/main/java/.../Service/
- Templates: src/main/resources/templates/
- Config: src/main/resources/application.properties

**Key Classes:**
- UserService (líneas 86-150: createUser)
- AdminService (líneas 250-300: validatePqrs)
- PqrsEntity (líneas 1-80: schema)

**DB Connection:**
PostgreSQL @PGHOST:PGPORT/PGDATABASE
Pool: HikariCP (max=5, min=2)

**APIs:**
- Cloudinary: /upload endpoints
- Groq: /chat/completions
```

### 9. Migración entre Sesiones

**Fin de sesión anterior:**
```
1. Generar distilled-notes.md
2. Guardar file-index.json
3. Archivar logs > 1MB
4. Purgar cache de archivos > 50KB
5. Crear session-handoff.md
```

**Inicio de nueva sesión:**
```
1. Leer session-handoff.md (qué se hizo)
2. Cargar distilled-notes.md (contexto comprimido)
3. Cargar file-index.json (qué archivos existen)
4. Preguntar: "Continuar desde...?"
5. Cargar solo contexto necesario
```

## Implementación en Mardique

**Directorio de memoria:**
```
.project-memory/
├── distilled-notes.md          # Resumen de cambios
├── session-handoff.md          # Traspaso de contexto
├── quick-reference.md          # Guía rápida
├── cache/
│   ├── file-index.json
│   ├── dependency-graph.json
│   ├── api-endpoints.json
│   └── entity-schema.json
└── archives/
    └── old-sessions/           # Historial
```

**Comandos útiles:**
```bash
# Mostrar estado de memoria
cat .project-memory/distilled-notes.md

# Cargar contexto de última sesión
# (automático en próxima sesión)

# Limpiar cache viejo
rm -rf .project-memory/cache/* 
# (regenerar con nuevo escaneo)
```

## Checklist de Memory Optimization

- [ ] Crear .project-memory/distilled-notes.md
- [ ] Crear file-index.json con archivos grandes
- [ ] Limitar chunks a máximo 30KB por lectura
- [ ] Cachear dependency-graph
- [ ] Usar quick-reference para contexto permanente
- [ ] Monitorear tamaño de sesión
- [ ] Ejecutar distill cada 30 minutos
- [ ] Archivar sesiones completadas