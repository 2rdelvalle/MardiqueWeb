---
name: code-distillation
triggers: ["terminar tarea", "refactorizar", "guardar sesión", "listo", "terminé", "completado", "finalizado", "hecho", "guardado"]
description: Ejecutar automáticamente al concluir una refactorización mayor, adición de features o corrección de bugs para actualizar el archivo .project-memory/distilled-notes.md.
---

# Code Distillation Skill

## Propósito
Resumir cambios clave después de tareas largas para evitar re-lectura completa en sesiones futuras.

## Cómo Usarlo

### Paso 1: Después de una Tarea Grande
Cuando termines de:
- Refactorizar un módulo completo
- Agregar nuevas features
- Solucionar bugs complejos
- Reescribir servicios

### Paso 2: Ejecutar Distill
Crea o actualiza `.project-memory/distilled-notes.md`:

```markdown
## Distilled Knowledge - [DATE]

### Cambios Realizados
- Archivo modificado: src/main/java/com/example/MardiqueWeb/Controller/AdminController.java
  - Agregado: Validación de permisos en línea 45
  - Eliminado: Llamada redundante a BD en línea 78
  - Impacto: Reduce queries en 30%

- Archivo: src/main/resources/templates/AdminDashboard.html
  - Actualizado: Template con nueva estructura de datos
  - Breaking change: El JSON devuelto ahora incluye campo 'status'

### Dependencias Nuevas
- com.google.zxing:core:3.5.3 (códigos QR)
- Requiere: Java 21+

### Decisiones Arquitectónicas
- Spring Security: Implementar roles granulares (Admin, Editor, User, Intranet)
- BD: Pool de conexiones HikariCP con max-pool-size=5

### Deuda Técnica Agregada
- [ ] TODO: Refactorizar UserService (muy grande, 500+ líneas)
- [ ] TODO: Agregar tests unitarios para PqrsService

### Próximas Sesiones
1. Ejecutar tests: `./mvnw test`
2. Revisar logs en `build.log`
3. Validar conexión PostgreSQL
```

### Paso 3: Guardar en Repositorio
El archivo va en:
```
.project-memory/
├── distilled-notes.md
├── architecture-decisions.md
└── quick-reference.md
```

## Ventajas
✅ La IA no re-lee 100 archivos  
✅ Contexto comprimido en 2KB  
✅ Decisiones documentadas  
✅ Deuda técnica visible  

## Integración con Sesiones Futuras
```
Usuario: "Continúa desde la última sesión"
IA: Lee .project-memory/distilled-notes.md primero
IA: Entiende cambios sin re-leer código
IA: Retoma trabajo en 2 minutos vs 30 minutos
```

## Template Mínimo
```markdown
## Distilled - [YYYY-MM-DD HH:MM]

**Files Changed:** 5
**Lines Modified:** 237
**Key Changes:**
- (brief bullet point)

**Next Steps:**
- (action items)
```

## Notas
- Actualizar después de CADA tarea completada
- Máximo 50 líneas por distilled note
- Incluir SHAs de commits cuando sea posible