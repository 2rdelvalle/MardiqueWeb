---
name: chunk-reading
triggers: ["archivo grande", "líneas", "optimizar método", "clase extensa", "método", "función", "modificar", "editar", "refactorizar"]
description: Usar obligatoriamente de forma autónoma siempre que se deba leer, analizar o modificar un archivo de código que supere las 500 líneas, leyendo únicamente el chunk o fragmento relevante.
---

# Chunk Reading Skill

## Propósito
Leer archivos grandes por fragmentos (chunks) en lugar de reescribir todo el archivo de una vez. Evita timeouts y optimiza uso de tokens.

## Problema
- Archivo de 2,000 líneas toma 100+ tokens solo en lectura
- Reescribir todo = desperdicio de contexto
- Límites dinámicos en Free tier causan bloqueos

## Solución: Lectura por Chunks

### Estrategia
1. **Identificar tamaño del archivo**
2. **Dividir en secciones lógicas** (clases, métodos, bloques)
3. **Leer chunk por chunk**
4. **Hacer cambios en el chunk específico**
5. **Mantener el resto sin tocar**

## Ejemplo Práctico

### ❌ MAL - Reescribir todo
```java
// UserService.java - 1,500 líneas
// Archivo COMPLETO se lee y reescribe
```

### ✅ BIEN - Leer por chunks

**Paso 1: Identificar secciones**
```
src/main/java/com/example/MardiqueWeb/Service/UserService.java
├── Imports (líneas 1-15)
├── Clase declaration (línea 17-25)
├── Fields (líneas 26-40)
├── Constructor (líneas 41-60)
├── Method: getUserById (líneas 61-85)
├── Method: createUser (líneas 86-150)
├── Method: updateUser (líneas 151-220)
├── Method: deleteUser (líneas 221-245)
└── Private helpers (líneas 246-1500)
```

**Paso 2: Leer solo el chunk que necesito**
```
Usuario: "Necesito optimizar el método getUserById"

IA: 
1. Leo líneas 61-85 (solo ese método)
2. Analizo la lógica
3. Propongo optimización
4. Actualizo SOLO esas líneas
5. El resto del archivo NO se toca
```

## Comando de Chunk Reading

```markdown
## Usar este formato cuando trabajes con archivos grandes:

**File:** src/main/java/com/example/MardiqueWeb/Service/AdminService.java
**Total Lines:** 800
**Focus Area:** Method `validatePqrsPermissions` (líneas 250-300)
**Action:** Read chunk + optimize + update

### Lectura del chunk:
- Líneas 250-300: Método a optimizar
- Context adicional: Líneas 245-305 (para ver relaciones)

### Cambios:
- Solo reescribir líneas 250-300
- Preservar líneas 1-249 y 301-800
```

## Tipos de Chunks

| Tipo | Líneas | Ejemplo |
|------|--------|---------|
| **Method** | 20-100 | Un solo método de una clase |
| **Class** | 200-400 | Una clase completa con todos sus métodos |
| **Package** | 50-150 | Grupo de métodos en una sección |
| **Section** | 10-50 | Imports, fields, constructors |

## Implementación en Proyecto Mardique

### Archivos grandes a tratar con chunks:

**1. Controllers** (500-1000 líneas típicas)
```
src/main/java/com/example/MardiqueWeb/Controller/AdminController.java
src/main/java/com/example/MardiqueWeb/Controller/IntranetController.java
```

**2. Services** (800-2000 líneas)
```
src/main/java/com/example/MardiqueWeb/Service/UserService.java
src/main/java/com/example/MardiqueWeb/Service/AdminService.java
src/main/java/com/example/MardiqueWeb/Service/DocumentService.java
```

**3. Templates Thymeleaf** (40-100KB)
```
src/main/resources/templates/AdminDashboard.html
src/main/resources/templates/IntranetDocumentos.html
src/main/resources/templates/Contacto.html
```

## Instrucciones para la IA

### Cuando veas un archivo > 500 líneas:

1. **NO intentes leer todo**
2. **Pregunta qué sección necesitas**
3. **Lee solo ese chunk**
4. **Actualiza solo ese chunk**
5. **Preserva el resto**

### Sintaxis de lectura con chunks:
```
Necesito optimizar [ARCHIVO]
- Líneas [X-Y]: [descripción del cambio]
- Context: líneas [X-20] a [Y+20]
- Preservar: resto del archivo
```

## Beneficios
✅ Ahorra 80% de tokens en archivos grandes  
✅ Evita timeouts en conexiones lentas  
✅ Cambios más precisos sin regresiones  
✅ Sesiones más rápidas en Free tier  

## Ejemplo Real en Mardique

**Tarea:** "Agregar validación a UserService.createUser()"

**Chunk Reading:**
```
Archivo: UserService.java
Tamaño: 1,200 líneas
Chunk objetivo: método createUser (líneas 86-150)

Lectura:
- Líneas 1-15: Imports (rapido escaneo)
- Líneas 17-40: Clase + fields (rapido escaneo)
- Líneas 80-160: MÉTODO COMPLETO (lectura detallada)

Cambios:
- Actualizar líneas 86-150
- Resto del archivo: sin cambios
```

## Checklist para Chunk Reading

- [ ] Identificar tamaño del archivo
- [ ] Determinar línea de inicio/fin del chunk
- [ ] Leer solo el chunk relevante
- [ ] Hacer cambios en ese chunk
- [ ] Verificar no hay dependencias rotas
- [ ] Confirmar resto del archivo intacto