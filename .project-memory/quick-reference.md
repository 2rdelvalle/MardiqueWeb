# Quick Reference - Mardique Project

## 📍 Rutas Clave

```
src/main/java/com/example/MardiqueWeb/
├── Config/          → SecurityConfig, WebConfig, DataInitializer
├── Controller/      → 16 controllers (Intranet*, Admin*, Editor*)
├── Entity/          → 34 entidades JPA
├── Repository/      → 34 repositorios Spring Data
└── Service/         → 16 servicios (PdfService, CloudinaryService, etc.)

src/main/resources/
├── templates/       → 33 archivos Thymeleaf HTML
├── static/css/      → 5 archivos CSS (portal, inicio, panel-nav, auth, chatbot)
└── application.properties → Configuración completa
```

## 🔑 Variables de Entorno

```bash
# Base de datos
PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD

# Cloudinary (uploads)
CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET

# Groq API (chatbot)
GROQ_API_KEY

# App
ENCRYPTION_KEY, INTRANET_UPLOAD_DIR
```

## 🏗️ Arquitectura

- **Roles:** ADMIN, EDITOR (sin ROLE_USER)
- **Security:** Spring Security 6, CSRF ignorado para /api/intranet/**
- **DB:** PostgreSQL, Hibernate ddl-auto=update
- **Templates:** Thymeleaf (HTML)
- **Uploads:** Cloudinary (secure_url)
- **PDF:** LibreOffice + PDFBox + OpenPDF

## 🎨 Brand Colors

```css
--pd: #1b4562  /* Primary Dark - Navy */
--pb: #28648c  /* Primary Blue */
--ag: #f09a36  /* Accent Gold */
--ac: #00a3e0  /* Accent Cyan */
```

## 📝 Comandos Útiles

```bash
# Build
set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.12" && call mvnw.cmd -B clean compile -q

# Test
set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.12" && call mvnw.cmd -B test -q

# Package
set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.12" && call mvnw.cmd -B package -DskipTests -q
```

## ⚠️ Notas Importantes

- `HrStaff.group` → mapeado a `staff_group` (reservado PostgreSQL)
- `schema.sql` eliminado — Hibernate gestiona schema
- LibreOffice en `C:\Program Files\LibreOffice\program\soffice.exe`
- MOCK_USER: `Adelino Aragon` / `AdelinoAragon@spmardique.co`

---

*Última actualización: 2026-09-03*