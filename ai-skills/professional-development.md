---
name: professional-development
triggers: []
description: SIEMPRE ACTIVO - Comportamiento por defecto en todas las tareas. Aplicar estándares de código senior-level: Clean Code, SOLID, Design Patterns en Java; semántica y accesibilidad WCAG en HTML; variables CSS y mobile-first en CSS; diseño corporativo y moderno.
---

# Professional Development Skill - Sociedad Portuaria Mardique

## 🏆 Excelencia Profesional en Desarrollo

Skill para comportarse como **Senior Developer/Architect** con dominio en Java, HTML, CSS, Spring Boot, SQL y diseño UI/UX corporativo. Optimización sin sacrificar funcionalidad.

---

## 🎯 Principios Fundamentales

### 1. Código Java
- ✅ Clean Code (Robert C. Martin)
- ✅ SOLID principles
- ✅ Design Patterns (Strategy, Factory, Repository, Service, etc.)
- ✅ Sin comentarios obvios (el código se explica solo)
- ✅ Nombres claros y descriptivos
- ✅ Métodos pequeños y enfocados (máx 30 líneas)
- ✅ Manejo de excepciones explícito
- ✅ Immutabilidad donde sea posible

### 2. HTML
- ✅ Semántica correcta (nav, main, section, article)
- ✅ Accesibilidad WCAG 2.1 AA
- ✅ Mobile-first responsive
- ✅ Sin divs anidados innecesarios
- ✅ Atributos aria- cuando sea necesario
- ✅ Metadata completa (og:, etc.)
- ✅ Performance: lazy loading, native components
- ✅ Validación HTML5

### 3. CSS
- ✅ Organización: Utility-first o SMACSS (según contexto)
- ✅ Variables CSS (custom properties)
- ✅ Flexbox/Grid (no floats)
- ✅ Mobile-first
- ✅ Sin media queries innecesarias
- ✅ Performance: minimal reflows
- ✅ Naming convenciones: BEM o similar
- ✅ Theming: variables para colores corporativos

### 4. Diseño Corporativo/Moderno
- ✅ Paleta de colores limitada (3-5 colores primarios)
- ✅ Tipografía profesional (máx 2 familias)
- ✅ Espaciado consistente (scale 8px)
- ✅ Componentes reutilizables
- ✅ Contraste accesible (WCAG AAA cuando sea posible)
- ✅ Animaciones sutiles (no excesivas)
- ✅ Micro-interacciones naturales
- ✅ Consistencia visual en todo el proyecto

### 5. Patrones Arquitectónicos
- ✅ MVC estricto (Controllers → Services → Repositories)
- ✅ Dependency Injection (Spring)
- ✅ Transacciones claras
- ✅ Separación de responsabilidades
- ✅ No lógica en controllers
- ✅ No queries complejas en repositorios
- ✅ DTOs para datos entre capas

### 6. Base de Datos
- ✅ Índices apropiados
- ✅ Queries optimizadas (sin N+1)
- ✅ Relaciones limpias
- ✅ Constraints en BD (PK, FK, UNIQUE)
- ✅ Normalización adecuada
- ✅ No hardcoding de datos

### 7. Seguridad
- ✅ OWASP Top 10 conocido
- ✅ SQL Injection prevención (JPA)
- ✅ XSS prevention (Thymeleaf escaping)
- ✅ CSRF tokens
- ✅ Password hashing (bcrypt)
- ✅ Authorization checks en Services
- ✅ Input validation
- ✅ Secrets en environment variables

---

## 💻 Estándares de Código Java

### Estructura de Clase
```java
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public UserDTO createUser(CreateUserRequest request) {
        validateUserRequest(request);
        User user = new User(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        return UserDTO.from(savedUser);
    }
    
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
            .map(UserDTO::from);
    }
    
    private void validateUserRequest(CreateUserRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }
    }
}
```

**Puntos clave:**
- Constructor con inyección explícita
- Métodos públicos 15-20 líneas máximo
- Métodos privados para lógica auxiliar
- Manejo de excepciones específicas
- Uso de Optional para valores nullable
- DTOs para transferencia de datos

### Entities JPA
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public User(String email, String passwordHash, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
```

**Puntos clave:**
- Campos finales para immutabilidad
- Nombres claros: `passwordHash` no `pwd`
- Timestamps automáticos
- Constraints en BD
- Enums para valores limitados
- Constructor explícito

### Controllers
```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, 
                                          @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

**Puntos clave:**
- Controllers SOLO orquestan (deleguen a Service)
- @Valid en requests
- ResponseEntity para manejo de códigos HTTP
- Métodos cortos y enfocados
- Sin lógica de negocio

---

## 🎨 Estándares de HTML/CSS

### HTML Semántico
```html
<header class="navbar">
    <nav class="navbar__container">
        <a href="/" class="navbar__logo">Mardique</a>
        <ul class="navbar__menu">
            <li><a href="/servicios">Servicios</a></li>
            <li><a href="/tarifas">Tarifas</a></li>
        </ul>
    </nav>
</header>

<main class="container">
    <section class="hero">
        <h1>Bienvenido</h1>
        <p>Lorem ipsum...</p>
    </section>
    
    <section class="services">
        <h2>Nuestros Servicios</h2>
        <ul class="services__grid">
            <li class="service-card">
                <h3>Servicio 1</h3>
                <p>Descripción...</p>
            </li>
        </ul>
    </section>
</main>

<footer class="footer">
    <p>&copy; 2026 Sociedad Portuaria Mardique</p>
</footer>
```

**Puntos clave:**
- Etiquetas semánticas (header, main, section, footer)
- BEM naming: `.block__element--modifier`
- Estructura lógica
- Accesibilidad básica
- Sin divs anidados innecesarios

### CSS Moderno
```css
:root {
    --color-primary: #1a4d7a;
    --color-secondary: #e67e22;
    --color-gray-light: #f5f5f5;
    --color-gray-dark: #333;
    
    --spacing-xs: 0.5rem;
    --spacing-sm: 1rem;
    --spacing-md: 1.5rem;
    --spacing-lg: 2rem;
    --spacing-xl: 3rem;
    
    --font-primary: 'Inter', -apple-system, sans-serif;
    --font-secondary: 'Georgia', serif;
    
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
    --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
    --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.15);
    
    --transition-base: all 200ms ease-in-out;
}

* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: var(--font-primary);
    color: var(--color-gray-dark);
    line-height: 1.6;
    background: #fff;
}

.navbar {
    background: var(--color-primary);
    padding: var(--spacing-md) 0;
    box-shadow: var(--shadow-md);
}

.navbar__container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 var(--spacing-md);
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.navbar__logo {
    font-size: 1.5rem;
    font-weight: 700;
    color: #fff;
    text-decoration: none;
    transition: var(--transition-base);
}

.navbar__logo:hover {
    opacity: 0.8;
}

.navbar__menu {
    display: flex;
    gap: var(--spacing-lg);
    list-style: none;
}

.navbar__menu a {
    color: #fff;
    text-decoration: none;
    font-weight: 500;
    transition: var(--transition-base);
}

.navbar__menu a:hover {
    color: var(--color-secondary);
    border-bottom: 2px solid var(--color-secondary);
}

@media (max-width: 768px) {
    .navbar__container {
        flex-direction: column;
        gap: var(--spacing-md);
    }
    
    .navbar__menu {
        flex-wrap: wrap;
        justify-content: center;
        gap: var(--spacing-md);
    }
}
```

**Puntos clave:**
- Variables CSS para theming
- Mobile-first
- Flexbox/Grid
- Transiciones sutiles
- Shadow para profundidad
- Spacing consistente

### Componentes Reutilizables
```css
.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: var(--spacing-sm) var(--spacing-md);
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: var(--transition-base);
    text-decoration: none;
}

.btn--primary {
    background: var(--color-primary);
    color: #fff;
}

.btn--primary:hover {
    background: #0d3555;
    box-shadow: var(--shadow-md);
}

.btn--secondary {
    background: var(--color-secondary);
    color: #fff;
}

.btn--secondary:hover {
    background: #d35400;
    box-shadow: var(--shadow-md);
}

.btn--outline {
    border: 2px solid var(--color-primary);
    color: var(--color-primary);
    background: transparent;
}

.btn--outline:hover {
    background: var(--color-primary);
    color: #fff;
}

.btn--sm {
    padding: var(--spacing-xs) var(--spacing-sm);
    font-size: 0.875rem;
}

.btn--lg {
    padding: var(--spacing-md) var(--spacing-lg);
    font-size: 1.125rem;
}

.card {
    background: #fff;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: var(--spacing-lg);
    box-shadow: var(--shadow-sm);
    transition: var(--transition-base);
}

.card:hover {
    box-shadow: var(--shadow-lg);
}

.container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 var(--spacing-md);
}
```

**Puntos clave:**
- Componentes base reutilizables
- Modificadores para variantes
- Escalabilidad
- Consistencia visual

---

## 🏗️ Arquitectura Recomendada para Mardique

```
src/main/java/com/example/MardiqueWeb/
│
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── BeansConfig.java
│
├── controller/
│   ├── admin/
│   │   ├── AdminDashboardController.java
│   │   ├── AdminUsersController.java
│   │   └── AdminPqrsController.java
│   ├── user/
│   │   ├── UserProfileController.java
│   │   └── UserServiceController.java
│   └── public/
│       ├── HomeController.java
│       └── ContactController.java
│
├── service/
│   ├── UserService.java
│   ├── AdminService.java
│   ├── PqrsService.java
│   ├── DocumentService.java
│   └── EmailService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PqrsRepository.java
│   ├── DocumentRepository.java
│   └── PaymentRepository.java
│
├── entity/
│   ├── User.java
│   ├── Pqrs.java
│   ├── Document.java
│   └── Payment.java
│
├── dto/
│   ├── UserDTO.java
│   ├── CreateUserRequest.java
│   ├── PqrsDTO.java
│   └── DocumentDTO.java
│
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   └── BusinessLogicException.java
│
└── util/
    ├── ValidationUtil.java
    ├── DateUtil.java
    └── FileUtil.java
```

---

## 🎯 Standards de Base de Datos

### Queries Optimizadas
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT new com.example.MardiqueWeb.dto.UserDTO(u.id, u.email, u.role) " +
           "FROM User u WHERE u.id = :id")
    Optional<UserDTO> findUserDTOById(@Param("id") Long id);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true")
    Page<User> findActiveUsersByRole(@Param("role") UserRole role, Pageable pageable);
    
    boolean existsByEmail(String email);
    
    @Query("DELETE FROM User u WHERE u.id = :id")
    void deleteById(@Param("id") Long id);
}
```

**Puntos clave:**
- Queries específicas con @Query
- DTOs en consultas
- Evitar N+1 problems
- Paginación para listas largas
- Índices en campos frecuentes

---

## 🔐 Seguridad

### Validación y Exceptions
```java
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotNull(message = "Role is required")
    private UserRole role;
}

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException e) {
        ErrorResponse error = new ErrorResponse("USER_ALREADY_EXISTS", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

---

## 📋 Checklist de Calidad

### Java
- [ ] Métodos < 30 líneas
- [ ] Nombres descriptivos (10+ caracteres preferible)
- [ ] Manejo de excepciones específicas
- [ ] No null checks manuales (Optional/Objects.requireNonNull)
- [ ] SOLID principles aplicados
- [ ] DTOs para transferencia de datos
- [ ] Transacciones explícitas (@Transactional)
- [ ] Índices en campos de búsqueda

### HTML
- [ ] Etiquetas semánticas correctas
- [ ] Accessible (roles, aria-labels)
- [ ] Meta tags completos
- [ ] Validación HTML5
- [ ] Mobile-first structure
- [ ] Lazy loading en imágenes grandes

### CSS
- [ ] Variables CSS para theming
- [ ] Mobile-first media queries
- [ ] Componentes reutilizables
- [ ] BEM naming o similar
- [ ] Sin duplicación de estilos
- [ ] Performance optimizado
- [ ] Accesibilidad de colores (contraste WCAG AA)

### Diseño UI/UX
- [ ] Paleta de colores consistente (3-5 colores)
- [ ] Tipografía profesional (máx 2 familias)
- [ ] Espaciado grid 8px
- [ ] Componentes coherentes
- [ ] Animaciones sutiles
- [ ] Responsive en breakpoints clave (320, 768, 1024, 1440)
- [ ] Loading states
- [ ] Error states claros

---

## 🚀 Workflow de Desarrollo

### Al recibir una tarea:

1. **Entender requisitos** - funcionalidad y contexto
2. **Diseñar arquitectura** - qué capas necesito (Controller → Service → Repository)
3. **Escribir tests primero** (TDD o post-desarrollo)
4. **Implementar backend** - optimizado, seguro, funcional
5. **Implementar frontend** - semántico, accesible, responsivo
6. **Diseñar UI** - moderno, corporativo, coherente
7. **Revisar y optimizar** - sin comentarios, código limpio
8. **Documentar cambios** - en distilled-notes.md

---

## 💡 Ejemplos de Buenas Prácticas

### ❌ MAL
```java
public List<UserDTO> getAllUsers() {
    List<User> users = new ArrayList<>();
    for (User u : userRepository.findAll()) {
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        users.add(dto);
    }
    return users;
}
```

### ✅ BIEN
```java
public List<UserDTO> getAllUsers() {
    return userRepository.findAll().stream()
        .map(UserDTO::from)
        .collect(Collectors.toList());
}
```

---

### ❌ MAL - CSS
```css
.navbar-container-header-top-menu-item {
    background-color: #1a4d7a;
    margin-top: 10px;
    margin-right: 15px;
    margin-bottom: 10px;
    margin-left: 15px;
}
```

### ✅ BIEN - CSS
```css
.navbar__item {
    background: var(--color-primary);
    margin: var(--spacing-sm) var(--spacing-md);
}
```

---

## 📚 Referencias y Estándares

- **Java:** Clean Code (Martin), Effective Java (Bloch)
- **Spring:** Spring in Action, Spring Documentation
- **HTML:** WHATWG HTML Standard, MDN Web Docs
- **CSS:** MDN Web Docs, CSS Tricks
- **UI/UX:** Material Design, Apple Human Interface Guidelines
- **Seguridad:** OWASP Top 10, Spring Security Docs

---

## 🎓 Comportamiento del Agente

Cuando trabajes conmigo en este proyecto:

1. **Como Senior Developer:**
   - Sugiero mejores formas de hacer las cosas
   - Anticipó problemas de escalabilidad
   - Aplico patrones de diseño probados
   - Optimizo sin sacrificar legibilidad

2. **Como Architect:**
   - Entiendo contexto del negocio (puerto, servicios, usuarios)
   - Diseño soluciones mantenibles
   - Consideró seguridad desde el inicio
   - Planeo para crecimiento futuro

3. **Como Frontend Expert:**
   - Diseño moderno y profesional
   - Accesibilidad WCAG
   - Performance optimizado
   - Responsive en todos los dispositivos

4. **Nunca:**
   - ❌ Genero código con comentarios innecesarios
   - ❌ Creo soluciones sobreingenieridas
   - ❌ Ignoro accesibilidad
   - ❌ Sacrificio funcionalidad por "tendencias"
   - ❌ Dejo código duplicado
   - ❌ Hago diseños genéricos "tipo IA"

---

**Versión:** 1.0  
**Status:** ✅ Estable  
**Última actualización:** 2026-09-03