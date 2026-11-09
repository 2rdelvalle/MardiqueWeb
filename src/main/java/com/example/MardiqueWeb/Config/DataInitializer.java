package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.UserRepository;
import com.example.MardiqueWeb.Repository.SystemConfigRepository;
import com.example.MardiqueWeb.Repository.FaqRepository;
import com.example.MardiqueWeb.Repository.HrSessionRepository;
import com.example.MardiqueWeb.Repository.HrStaffRepository;
import com.example.MardiqueWeb.Repository.DocumentDepartmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigRepository systemConfigRepository;
    private final FaqRepository faqRepository;
    private final HrSessionRepository hrSessionRepository;
    private final HrStaffRepository hrStaffRepository;
    private final DocumentDepartmentRepository docDeptRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           SystemConfigRepository systemConfigRepository,
                           FaqRepository faqRepository,
                           HrSessionRepository hrSessionRepository,
                           HrStaffRepository hrStaffRepository,
                           DocumentDepartmentRepository docDeptRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemConfigRepository = systemConfigRepository;
        this.faqRepository = faqRepository;
        this.hrSessionRepository = hrSessionRepository;
        this.hrStaffRepository = hrStaffRepository;
        this.docDeptRepository = docDeptRepository;
    }

    private void createUserIfNotExists(String username, String rawPassword, String email, String nombres, String apellidos, String role, String departamento) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User u = new User(username, passwordEncoder.encode(rawPassword), email, nombres, apellidos, null, role);
            u.setDepartamento(departamento);
            userRepository.save(u);
        }
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin123", "admin@mardique.com", "Admin", "TI", "ROLE_ADMIN", null);
        createUserIfNotExists("admin_comercial", "admin123", "comercial@mardique.com", "Admin", "Comercial", "ROLE_ADMIN", "Comercial");
        createUserIfNotExists("admin_juridica", "admin123", "juridica@mardique.com", "Admin", "Jurídica", "ROLE_ADMIN", "Jurídica");
        createUserIfNotExists("admin_compras", "admin123", "compras@mardique.com", "Admin", "Compras", "ROLE_ADMIN", "Compras");
        createUserIfNotExists("admin_it", "admin123", "it@mardique.com", "Admin", "IT", "ROLE_ADMIN", "IT");
        createUserIfNotExists("editor", "editor123", "editor@mardique.com", "Edit", "Editor", "ROLE_EDITOR", null);

        seedConfigIfAbsent("TARIFA_IFRAME_URL", "", "URL del iframe para la página de Tarifas (Google Sheets, SharePoint, PDF, etc.)");
        seedConfigIfAbsent("TARIFA_PHONE", "(57) (5) 669 0730", "Teléfono de contacto para información de tarifas");
        seedConfigIfAbsent("TARIFA_EMAIL", "info@spmardique.com", "Correo de contacto para información de tarifas");

        if (faqRepository.count() == 0) {
            seedFaqs();
        }
        if (hrSessionRepository.count() == 0) {
            seedHrSessions();
        }
        if (hrStaffRepository.count() == 0) {
            seedHrStaff();
        }
        if (docDeptRepository.count() == 0) {
            seedDocDepartments();
        }
    }

    private void seedFaqs() {
        faqRepository.save(new Faq("¿Qué servicios ofrece Mardique?",
            "Mardique ofrece servicios portuarios multipropósito: **carga general, gráneles, contenedores, hidrocarburos y grúas móviles**, además de infraestructura y logística portuaria para el desarrollo del país."));
        faqRepository.save(new Faq("¿Dónde está ubicada la empresa?",
            "Estamos ubicados en **Km 7 Vía a Mamonal, Cartagena, Colombia**. Teléfono PBX: **(57) (5) 669 0730**."));
        faqRepository.save(new Faq("¿Cómo puedo contactar al Gerente Comercial?",
            "Para contactar al **Gerente Comercial**, agenda una cita o solicita información a través del formulario de contacto del chat y un representante te atenderá."));
        faqRepository.save(new Faq("¿Cómo radico una PQRS?",
            "Puedes radicar tu petición, queja, reclamo o solicitud en la sección **Contacto** de nuestra página web, llenando el formulario de PQRS."));
        faqRepository.save(new Faq("¿Cuáles son los horarios de atención?",
            "Nuestras operaciones portuarias se realizan las **24 horas, los 365 días del año**. Las oficinas administrativas atienden en horario hábil de lunes a viernes."));
        faqRepository.save(new Faq("¿Qué se necesita para inscribirse como usuario?",
            "Para inscribirte debes contactar al **Oficial de Cumplimiento** mediante el formulario de contacto del chat (área Inscripción de Usuarios), quien te indicará los requisitos documentales según tu perfil (cliente o proveedor)."));
        faqRepository.save(new Faq("¿Cómo solicito información de tarifas?",
            "Para información de tarifas, agenda una cita o solicita información a través del formulario de contacto del chat (área **Gerente Comercial**) y nuestro equipo te atenderá."));
        faqRepository.save(new Faq("¿Cómo contacto con Talento Humano?",
            "Para temas de empleo, pasantías y trámites de personal, agenda una cita o solicita información a través del formulario del chat (área **Talento Humano**)."));
        faqRepository.save(new Faq("¿Qué trámites puedo realizar en línea?",
            "Puedes descargar formatos de solicitudes de servicio, inscripciones y otros trámites en la sección **Trámites en Línea** de nuestra página web."));
        faqRepository.save(new Faq("¿Quién atiende la documentación aduanera?",
            "La **Documentación Aduanera** tiene un área especializada. Agenda una cita o solicita información a través del formulario de contacto del chat y un representante te atenderá."));
    }

    private void seedConfigIfAbsent(String key, String value, String description) {
        if (systemConfigRepository.findByConfigKey(key).isEmpty()) {
            SystemConfig sc = new SystemConfig();
            sc.setConfigKey(key);
            sc.setConfigValue(value);
            sc.setDescription(description);
            systemConfigRepository.save(sc);
        }
    }

    private void seedHrSessions() {
        HrSession s1 = new HrSession();
        s1.setSectionId("reglamento");
        s1.setTitle("Reglamento Interno de Trabajo");
        s1.setDescription("Conoce las normas, derechos y obligaciones que regulan la relación laboral en la empresa.");
        s1.setIcon("fas fa-gavel");
        s1.setIconClass("reg");
        s1.setSolicitable(false);
        s1.setSortOrder(1);
        hrSessionRepository.save(s1);

        HrSession s2 = new HrSession();
        s2.setSectionId("permisos");
        s2.setTitle("Solicitud de Permiso");
        s2.setDescription("Gestiona permisos personales, médicos, académicos y de cualquier tipo de forma digital.");
        s2.setIcon("fas fa-file-signature");
        s2.setIconClass("perm");
        s2.setSolicitable(true);
        s2.setRequiredRoles("empleado,jefe,talento");
        s2.setMaxRecipients(3);
        s2.setSortOrder(2);
        hrSessionRepository.save(s2);

        HrSession s3 = new HrSession();
        s3.setSectionId("vacaciones");
        s3.setTitle("Solicitud de Vacaciones");
        s3.setDescription("Planifica y solicita tus vacaciones siguiendo el proceso establecido por la empresa.");
        s3.setIcon("fas fa-umbrella-beach");
        s3.setIconClass("vac");
        s3.setSolicitable(true);
        s3.setRequiredRoles("empleado,jefe,gerente,talento");
        s3.setMaxRecipients(4);
        s3.setSortOrder(3);
        hrSessionRepository.save(s3);

        HrSession s4 = new HrSession();
        s4.setSectionId("compensacion-vacaciones");
        s4.setTitle("Formato de Compensación de Vacaciones");
        s4.setDescription("FT-GATH-SPM-010 — Gestiona el formato para la compensación de vacaciones pendientes.");
        s4.setIcon("fas fa-money-bill-transfer");
        s4.setIconClass("vac");
        s4.setSolicitable(true);
        s4.setRequiredRoles("empleado,gerente,talento");
        s4.setMaxRecipients(3);
        s4.setSortOrder(4);
        hrSessionRepository.save(s4);
    }

    private void seedHrStaff() {
        saveStaff("AdelinoAragon@spmardique.co", "Adelino Aragon", "Coordinador de Sistemas", "Coordinadores", 1);
        saveStaff("johnnier.mardique@mardique.com", "Johnnier Admin", "Administrador", "Administración", 2);
        saveStaff("gerente-general@mardique.com", "Gerente General", "Gerente General", "Gerencia", 3);
        saveStaff("gerente-operaciones@mardique.com", "Gerente de Operaciones", "Gerente de Operaciones", "Gerencia", 4);
        saveStaff("gerente-comercial@mardique.com", "Gerente Comercial", "Gerente Comercial", "Gerencia", 5);
        saveStaff("gerente-financiera@mardique.com", "Gerente Financiera", "Gerente Financiera", "Gerencia", 6);
        saveStaff("coordinador-sistemas@mardique.com", "Coordinador de Sistemas", "Coordinador de Sistemas", "Coordinadores", 7);
        saveStaff("coordinador-th@mardique.com", "Coordinador de Talento Humano", "Coordinador de Talento Humano", "Coordinadores", 8);
        saveStaff("talento-humano@mardique.com", "Talento Humano", "Talento Humano", "Talento Humano", 9);
    }

    private void saveStaff(String email, String name, String role, String group, int order) {
        if (!hrStaffRepository.existsByEmail(email)) {
            HrStaff s = new HrStaff();
            s.setEmail(email);
            s.setName(name);
            s.setRole(role);
            s.setGroup(group);
            s.setSortOrder(order);
            hrStaffRepository.save(s);
        }
    }

    private void seedDocDepartments() {
        seedDept("rrhh", "Talento Humano", "fa-users", 1);
        seedDept("ti", "Sistemas", "fa-laptop-code", 2);
        seedDept("finanzas", "Contabilidad", "fa-chart-line", 3);
    }

    private void seedDept(String slug, String name, String icon, int order) {
        if (!docDeptRepository.existsBySlug(slug)) {
            DocumentDepartment d = new DocumentDepartment();
            d.setSlug(slug);
            d.setName(name);
            d.setIcon(icon);
            d.setSortOrder(order);
            docDeptRepository.save(d);
        }
    }
}
