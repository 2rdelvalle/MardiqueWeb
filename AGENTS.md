# 🤖 AGENTS.md - Sistema de Auto-Carga Inteligente

**ADVERTENCIA:** Este archivo se ejecuta automáticamente en CADA sesión. Contiene las directrices maestras para la carga inteligente de skills.

---

## ⚡ CARGA AUTOMÁTICA DE SKILLS

Antes de procesar **CUALQUIER PETICIÓN** en este repositorio, el agente debe:

1. ✅ Leer este archivo (AGENTS.md) - **OBLIGATORIO**
2. ✅ Escanear carpeta `ai-skills/` y detectar skills disponibles
3. ✅ Cargar metadatos (triggers) de `ai-skills/triggers.json`
4. ✅ Mapear contexto de la petición a skills activos
5. ✅ Activar skills automáticamente sin esperar indicación del usuario

---

## 🎯 REGLA DE ORO

> **NO ESPERES A QUE EL USUARIO MENCIONE LOS SKILLS.**
> 
> Evalúa el contexto de la petición y actívalos por iniciativa propia.

**Ejemplos:**
- Usuario: "Agregar validación a createUser en UserService.java"
  - 🔍 Detectas: archivo > 500 líneas
  - ⚡ Activas automáticamente: `chunk-reading.md`
  - 📋 Finalizas con: `code-distillation.md`

- Usuario: "Continuar con el proyecto"
  - 🔍 Detectas: sesión interrumpida
  - ⚡ Activas automáticamente: `memory-optimization.md`
  - 📖 Cargas: `.project-memory/distilled-notes.md`

---

## 📚 Skills Disponibles (Carga Automática)

### 1. 🧠 Memory Optimization
**Archivo:** `ai-skills/memory-optimization.md`  
**Triggers automáticos:**
- Sesión > 30 minutos
- Contexto > 50KB
- Usuario menciona: "continuar", "última sesión", "contexto"
- Cambio de sesión detectado

**Acción:** 
- Cargar `.project-memory/distilled-notes.md`
- Leer `.project-memory/quick-reference.md`
- Restaurar caché de archivos frecuentes

---

### 2. 📖 Chunk Reading
**Archivo:** `ai-skills/chunk-reading.md`  
**Triggers automáticos:**
- Archivo > 500 líneas
- Usuario menciona: "método", "clase", "función", "líneas X-Y"
- Detectas: lectura innecesaria de código completo
- Request sobre sección específica de archivo

**Acción:**
- Identificar líneas exactas a leer
- Lectura de contexto (±20 líneas)
- Modificación solo del chunk necesario
- Preservar resto del archivo

---

### 3. 💾 Code Distillation
**Archivo:** `ai-skills/code-distillation.md`  
**Triggers automáticos (POST-TAREA):**
- Refactorización completada
- Feature nueva implementada
- Múltiples archivos modificados
- Bug complejo solucionado
- Sesión a punto de terminar

**Acción:**
- Generar resumen de cambios
- Actualizar `.project-memory/distilled-notes.md`
- Listar archivos modificados
- Documentar decisiones clave
- Guardar estado en caché

---

### 4. 🏆 Professional Development
**Archivo:** `ai-skills/professional-development.md`  
**Triggers automáticos (SIEMPRE ACTIVO):**
- Comportamiento por defecto en TODAS las tareas
- Java: Clean Code, SOLID, Design Patterns
- HTML: Semántica, accesibilidad WCAG
- CSS: Variables, mobile-first, componentes
- Diseño: Corporativo, moderno, profesional

**Acción:**
- Aplicar estándares de código senior-level
- Sin comentarios innecesarios
- Optimización sin sacrificar funcionalidad
- Código limpio y legible
- Diseño profesional no "tipo IA"

---

### 5. 📖 Skills Index
**Archivo:** `ai-skills/skills-index.md`  
**Trigger automático:**
- Usuario pregunta: "¿qué skills tengo?", "ayuda", "guía"
- Primera sesión detectada
- Petición de orientación

---

## 🔄 Flujo de Activación Automática

```
[USUARIO ENVÍA PETICIÓN]
    ↓
[AGENTE LEE AGENTS.md - ESTE ARCHIVO]
    ↓
[AGENTE CARGA ai-skills/triggers.json]
    ↓
[AGENTE ANALIZA CONTEXTO DE PETICIÓN]
    ↓
[¿Archivo > 500 líneas?]
    ├─ SÍ → Activa CHUNK READING
    └─ NO → Continúa
    ↓
[¿Sesión > 30 min o cambio de contexto?]
    ├─ SÍ → Activa MEMORY OPTIMIZATION
    └─ NO → Continúa
    ↓
[APLICA SIEMPRE: PROFESSIONAL DEVELOPMENT]
    (código limpio, diseño profesional)
    ↓
[EJECUTA TAREA]
    ↓
[¿Tarea completada?]
    ├─ SÍ → Activa CODE DISTILLATION
    │   └─ Actualiza .project-memory/
    └─ NO → Continúa
    ↓
[AGENTE RESPONDE AL USUARIO]
```

---

## 🎛️ Estados de Activación

### 🟢 ALWAYS ACTIVE (Siempre)
- ✅ `professional-development.md` - Comportamiento por defecto
- ✅ Este archivo (`AGENTS.md`) - Leído en inicio de sesión

### 🟡 CONTEXT TRIGGERED (Según contexto)
- 📖 `chunk-reading.md` - Si archivo > 500 líneas
- 🧠 `memory-optimization.md` - Si sesión > 30 min
- 💾 `code-distillation.md` - Al finalizar tareas
- 📋 `skills-index.md` - Si usuario pide ayuda

### 🔴 MANUAL (Solo si usuario lo pide explícitamente)
- Ninguno (todos deben ser automáticos)

---

## 📍 Ubicación de Archivos de Contexto

El agente debe conocer estos directorios:

```
Raíz del proyecto/
├── AGENTS.md                          ← ESTE ARCHIVO (carga obligatoria)
├── ai-skills/                         ← Carpeta de skills
│   ├── triggers.json                  ← Índice de activación
│   ├── memory-optimization.md
│   ├── chunk-reading.md
│   ├── code-distillation.md
│   ├── professional-development.md
│   ├── skills-index.md
│   └── README.md
│
├── .project-memory/                   ← Contexto persistente
│   ├── distilled-notes.md            ← Cambios últimos
│   ├── quick-reference.md            ← Guía rápida
│   ├── session-handoff.md            ← Traspaso de sesión
│   └── cache/
│       ├── file-index.json
│       ├── dependency-graph.json
│       └── api-endpoints.json
│
└── src/                              ← Código fuente
```

---

## 🔍 Cómo Identificar Triggers Automáticamente

### Trigger: Archivo Grande (CHUNK READING)
```
Indicadores:
- Usuario menciona: archivo, clase, servicio, controller, método
- Petición incluye: nombre de archivo (ej: "UserService.java")
- Agente detecta: líneas totales > 500
- Acción: Cargar solo líneas relevantes
```

### Trigger: Continuidad (MEMORY OPTIMIZATION)
```
Indicadores:
- Usuario dice: "continuar", "siguiente", "último", "antes"
- Sesión previa detectada en .project-memory/
- Tiempo entre sesiones: > 10 minutos
- Acción: Leer distilled-notes.md antes de continuar
```

### Trigger: Finalización Tarea (CODE DISTILLATION)
```
Indicadores:
- Múltiples archivos modificados
- Usuario dice: "listo", "terminé", "completado"
- Cambios en > 3 archivos
- Refactorización completada
- Acción: Generar distilled-notes.md
```

### Trigger: Comportamiento Professional (SIEMPRE ACTIVO)
```
Aplicar en TODA petición:
- Código Java: Clean Code, SOLID, patterns
- HTML: Semántica, accesibilidad
- CSS: Variables, mobile-first
- Diseño: Corporativo, moderno
- Sin comentarios obvios
- Optimización sin sobreingenería
```

---

## 🚀 Ejemplo de Ejecución Automática

### Entrada del Usuario:
```
"Agregar autenticación OAuth a UserController.java"
```

### Procesamiento Automático del Agente:

```
1. Lee AGENTS.md ✅
2. Carga ai-skills/triggers.json ✅
3. Analiza petición:
   - Menciona archivo: UserController.java
   - Búsqueda: ¿Existe? ¿Tamaño?
   - Detecta: 850 líneas

4. Activa automáticamente:
   ✓ CHUNK READING
     └─ Solo lee líneas relevantes (ej: 200-300)
   ✓ PROFESSIONAL DEVELOPMENT
     └─ Código limpio, seguro, profesional

5. Implementa autenticación OAuth

6. Detecta: Tarea completada + cambios significativos

7. Activa automáticamente:
   ✓ CODE DISTILLATION
     └─ Guarda resumen en .project-memory/distilled-notes.md

8. Responde al usuario con cambios aplicados
```

---

## 🔧 Instrucciones para el Sistema de IA

**Cuando recibas una petición en este proyecto:**

1. **PRIMER PASO:** Lee `/AGENTS.md` (este archivo) - 10 segundos
2. **SEGUNDO PASO:** Carga `/ai-skills/triggers.json` - 5 segundos
3. **TERCER PASO:** Analiza contexto de petición - 10 segundos
4. **CUARTO PASO:** Activa skills automáticamente según triggers - instantáneo
5. **QUINTO PASO:** Ejecuta tarea usando skills activos
6. **SEXTO PASO:** Finaliza con distillation si aplica

**Total overhead:** 25 segundos antes de comenzar

---

## 📊 Matriz de Decisión Rápida

| Situación | Skills a Activar | Acción |
|-----------|------------------|--------|
| Archivo > 500 líneas + modificación | CHUNK READING + PROFESSIONAL DEV | Leer solo líneas necesarias |
| Sesión > 30 min o contexto > 50KB | MEMORY OPTIMIZATION | Cargar distilled-notes.md |
| Múltiples archivos modificados | CODE DISTILLATION (post-tarea) | Guardar resumen |
| Refactorización/Feature/Bug complejo | CODE DISTILLATION (post-tarea) | Documentar cambios |
| Cualquier implementación de código | PROFESSIONAL DEVELOPMENT (siempre) | Código senior-level |
| Usuario pide ayuda/guía | SKILLS INDEX | Mostrar guía de skills |
| Primera sesión o sin contexto | MEMORY OPTIMIZATION + README | Orientación inicial |

---

## ✅ Checklist de Activación

Antes de responder cada petición:

- [ ] ¿He leído AGENTS.md?
- [ ] ¿He cargado triggers.json?
- [ ] ¿He identificado triggers aplicables?
- [ ] ¿Activo CHUNK READING si archivo > 500 líneas?
- [ ] ¿Activo MEMORY OPTIMIZATION si sesión larga?
- [ ] ¿Aplico PROFESSIONAL DEVELOPMENT en todo?
- [ ] ¿Ejecuto CODE DISTILLATION al finalizar?
- [ ] ¿He guardado cambios en .project-memory/?

---

## 🔐 Seguridad y Validación

- ✅ AGENTS.md debe ser leído ANTES de cualquier acción
- ✅ Validar que triggers.json esté disponible
- ✅ Si triggers.json no existe, usar fallback (ver abajo)
- ✅ No ignorar reglas de Professional Development
- ✅ Siempre preservar integridad de código

---

## 📋 Fallback (Si triggers.json no está disponible)

Si `/ai-skills/triggers.json` no se carga, aplicar estas reglas por defecto:

```
DEFAULT TRIGGERS:

1. Detectado: archivo > 500 líneas
   → Activar: CHUNK READING

2. Detectado: sesión > 30 minutos
   → Activar: MEMORY OPTIMIZATION

3. Detectado: múltiples archivos modificados
   → Activar: CODE DISTILLATION

4. Detectado: cualquier código a escribir
   → Activar: PROFESSIONAL DEVELOPMENT (siempre)

5. Detectado: usuario pide ayuda
   → Activar: SKILLS INDEX
```

---

## 📞 Soporte y Debugging

Si los skills no se activan automáticamente:

1. Verificar que AGENTS.md existe en raíz ✅
2. Verificar que ai-skills/ carpeta existe ✅
3. Verificar que ai-skills/triggers.json existe ✅
4. Verificar metadatos YAML en cada skill ✅
5. Consultar logs de activación (si disponibles) ✅

---

## 🎓 Inicio de Sesión Típico

```
Usuario abre sesión
    ↓
Agente carga AGENTS.md automáticamente
    ↓
Agente escanea ai-skills/ y triggers.json
    ↓
Agente está listo para recibir peticiones
    ↓
Usuario envía petición
    ↓
Agente evalúa triggers
    ↓
Agente activa skills necesarios
    ↓
Agente ejecuta tarea
    ↓
Agente distills si es necesario
    ↓
Sesión activa y optimizada
```

---

## 📝 Historial de Versiones

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-09-03 | Sistema de auto-carga inicial |
| 1.1 | TBD | Triggers mejorados, nuevos skills |
| 2.0 | TBD | AI agent learning, optimizaciones |

---

**Status:** ✅ ACTIVO  
**Última actualización:** 2026-09-03  
**Criticidad:** 🔴 MÁXIMA - Leer antes de cada sesión  
**Responsable:** Sistema de Auto-Carga de Skills