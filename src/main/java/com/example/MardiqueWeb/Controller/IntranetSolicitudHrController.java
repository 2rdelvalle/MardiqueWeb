package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.SolicitudHr;
import com.example.MardiqueWeb.Entity.SolicitudHrRecipient;
import com.example.MardiqueWeb.Entity.UserSignature;
import com.example.MardiqueWeb.Repository.SolicitudHrRepository;
import com.example.MardiqueWeb.Repository.SolicitudHrRecipientRepository;
import com.example.MardiqueWeb.Repository.UserSignatureRepository;
import com.example.MardiqueWeb.Service.CloudinaryService;
import com.example.MardiqueWeb.Service.ExcelToPdfService;
import com.example.MardiqueWeb.Service.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/intranet/solicitudes-hr")
public class IntranetSolicitudHrController {

    private static final Logger log = LoggerFactory.getLogger(IntranetSolicitudHrController.class);

    private final SolicitudHrRepository          solRepo;
    private final SolicitudHrRecipientRepository recipRepo;
    private final UserSignatureRepository        sigRepo;
    private final PdfService                     pdfService;
    private final CloudinaryService              cloudinaryService;
    private final ExcelToPdfService              excelToPdfService;

    public IntranetSolicitudHrController(SolicitudHrRepository solRepo,
                                         SolicitudHrRecipientRepository recipRepo,
                                         UserSignatureRepository sigRepo,
                                         PdfService pdfService,
                                         CloudinaryService cloudinaryService,
                                         ExcelToPdfService excelToPdfService) {
        this.solRepo           = solRepo;
        this.recipRepo         = recipRepo;
        this.sigRepo           = sigRepo;
        this.pdfService        = pdfService;
        this.cloudinaryService = cloudinaryService;
        this.excelToPdfService = excelToPdfService;
    }

    public static final Map<String, String> STAFF_DIRECTORY = Map.of(
            "AdelinoAragon@spmardique.co",       "Adelino Aragon",
            "johnnier.mardique@mardique.com",    "Johnnier Admin",
            "gerente-general@mardique.com",      "Gerente General",
            "gerente-operaciones@mardique.com",  "Gerente de Operaciones",
            "gerente-comercial@mardique.com",    "Gerente Comercial",
            "gerente-financiera@mardique.com",   "Gerente Financiera",
            "coordinador-sistemas@mardique.com", "Coordinador de Sistemas",
            "coordinador-th@mardique.com",       "Coordinador de Talento Humano",
            "talento-humano@mardique.com",       "Talento Humano"
    );

    public static final Map<String, String> STAFF_ROLES = Map.of(
            "AdelinoAragon@spmardique.co",       "Coordinador de Sistemas",
            "johnnier.mardique@mardique.com",    "Administrador",
            "gerente-general@mardique.com",      "Gerente General",
            "gerente-operaciones@mardique.com",  "Gerente de Operaciones",
            "gerente-comercial@mardique.com",    "Gerente Comercial",
            "gerente-financiera@mardique.com",   "Gerente Financiera",
            "coordinador-sistemas@mardique.com", "Coordinador de Sistemas",
            "coordinador-th@mardique.com",       "Coordinador de Talento Humano",
            "talento-humano@mardique.com",       "Talento Humano"
    );

    // =========================================================================
    // STAFF
    // =========================================================================

    @GetMapping("/staff")
    public List<Map<String, String>> getStaffDirectory() {
        List<Map<String, String>> list = new ArrayList<>();
        STAFF_DIRECTORY.forEach((email, name) -> {
            Map<String, String> item = new HashMap<>();
            item.put("email", email);
            item.put("name", name);
            item.put("role", STAFF_ROLES.getOrDefault(email, ""));
            list.add(item);
        });
        return list;
    }

    // =========================================================================
    // CREAR SOLICITUD
    // =========================================================================

    @PostMapping
    public ResponseEntity<?> createSolicitud(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("sectionId")    String sectionId,
            @RequestParam("title")        String title,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam("senderEmail")  String senderEmail,
            @RequestParam("senderName")   String senderName,
            @RequestParam(value = "recipients", required = false, defaultValue = "[]") String recipientsJson) {

        SolicitudHr sol = new SolicitudHr();
        sol.setSectionId(sectionId);
        sol.setTitle(title);
        sol.setDescription(description);
        sol.setSenderEmail(senderEmail);
        sol.setSenderName(senderName);
        sol.setStatus("PENDIENTE");
        sol.setCreatedAt(LocalDateTime.now());
        sol.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            try {
                String originalName = file.getOriginalFilename();
                sol.setDocumentName(originalName);

                // Subir el Excel original a Cloudinary
                String excelUrl = cloudinaryService.uploadFile(file);
                sol.setDocumentUrl(excelUrl);

                // Convertir a PDF y estampar firma del solicitante en slot 0
                try {
                    byte[] excelBytes = file.getBytes();
                    byte[] pdfBytes   = excelToPdfService.convertExcelToPdf(excelBytes, originalName);

                    Optional<UserSignature> senderSig = sigRepo.findByEmail(senderEmail);
                    if (senderSig.isPresent()
                            && senderSig.get().getSignatureUrl() != null
                            && !senderSig.get().getSignatureUrl().isEmpty()) {

                        String senderRole = STAFF_ROLES.getOrDefault(senderEmail, "");
                        List<PdfService.SigPosition> allPos = pdfService.detectAllSignaturePositions(pdfBytes);
                        log.info("Posiciones detectadas al crear: {} para remitente {}", allPos.size(), senderEmail);

                        // Slot 0 = firma del empleado/solicitante
                        PdfService.SigPosition pos = allPos.isEmpty()
                                ? new PdfService.SigPosition(130f, 100f, 120f, 35f, 1)
                                : allPos.get(0);

                        pdfBytes = pdfService.stampSignatureAtPosition(pdfBytes,
                                senderSig.get().getSignatureUrl(),
                                senderName, senderRole,
                                pos.page, pos.x, pos.y, pos.width, pos.height);

                        log.info("Firma del solicitante estampada en slot 0: x={} y={} pg={}",
                                pos.x, pos.y, pos.page);
                    } else {
                        log.info("Solicitante {} no tiene firma registrada, se omite estampado inicial", senderEmail);
                    }

                    sol.setPdfUrl("data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes));

                } catch (Exception pdfEx) {
                    log.warn("Pre-conversión/firma PDF falló, se convertirá bajo demanda: {}", pdfEx.getMessage(), pdfEx);
                }

            } catch (Exception e) {
                log.error("Error procesando archivo al crear solicitud: {}", e.getMessage(), e);
                sol.setDocumentName(file.getOriginalFilename());
            }
        }

        sol = solRepo.save(sol);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> recipientsList = mapper.readValue(recipientsJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            for (int i = 0; i < recipientsList.size(); i++) {
                Map<String, String> r = recipientsList.get(i);
                SolicitudHrRecipient recip = new SolicitudHrRecipient();
                recip.setSolicitudId(sol.getId());
                recip.setRecipientEmail(r.getOrDefault("email", ""));
                recip.setRecipientName(r.getOrDefault("name", ""));
                recip.setStatus(i == 0 ? "PENDIENTE" : "EN_ESPERA");
                recip.setOrderIndex(i);
                recip.setCreatedAt(LocalDateTime.now());
                recipRepo.save(recip);
            }
        } catch (Exception e) {
            log.warn("Error parsing recipients JSON: {}", e.getMessage());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id",      sol.getId());
        resp.put("status",  sol.getStatus());
        resp.put("message", "Solicitud enviada y ruta de aprobación configurada automáticamente");
        return ResponseEntity.ok(resp);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @GetMapping("/sent")
    public List<Map<String, Object>> getSent(@RequestParam String email) {
        List<SolicitudHr> sols = solRepo.findBySenderEmailOrderByCreatedAtDesc(email);
        return sols.stream().map(this::toMap).toList();
    }

    @GetMapping("/received")
    public List<Map<String, Object>> getReceived(@RequestParam String email) {
        List<SolicitudHrRecipient> recips = recipRepo.findByRecipientEmailOrderByCreatedAtDesc(email);
        Set<Long> solIds = new LinkedHashSet<>();
        for (SolicitudHrRecipient r : recips) solIds.add(r.getSolicitudId());

        List<SolicitudHr> senderSols = solRepo.findBySenderEmailOrderByCreatedAtDesc(email);
        for (SolicitudHr s : senderSols) solIds.add(s.getId());

        List<Long> allIds = new ArrayList<>(solIds);
        Map<Long, List<SolicitudHrRecipient>> recipMap = new HashMap<>();
        if (!allIds.isEmpty()) {
            recipRepo.findBySolicitudIds(allIds).forEach(r ->
                    recipMap.computeIfAbsent(r.getSolicitudId(), k -> new ArrayList<>()).add(r));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (!allIds.isEmpty()) {
            Map<Long, SolicitudHr> solMap = solRepo.findAllById(allIds).stream()
                    .collect(Collectors.toMap(SolicitudHr::getId, s -> s));
            for (Long solId : allIds) {
                SolicitudHr sol = solMap.get(solId);
                if (sol != null) {
                    Map<String, Object> m = toMap(sol);
                    m.put("recipients", recipMap.getOrDefault(solId, List.of())
                            .stream().map(this::toRecipMap).toList());
                    m.put("isSender", email.equals(sol.getSenderEmail()));
                    result.add(m);
                }
            }
        }
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSolicitud(@PathVariable Long id) {
        return solRepo.findById(id)
                .map(sol -> {
                    Map<String, Object> m = toDetailMap(sol);
                    List<SolicitudHrRecipient> recips = recipRepo.findBySolicitudIdOrderByCreatedAtDesc(id);
                    m.put("recipients", recips.stream().map(this::toRecipMap).toList());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // PDF
    // =========================================================================

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getSolicitudPdf(@PathVariable Long id) {
        return buildPdfResponse(id, false);
    }

    @GetMapping("/{id}/pdf/download")
    public ResponseEntity<byte[]> downloadSolicitudPdf(@PathVariable Long id) {
        return buildPdfResponse(id, true);
    }

    private ResponseEntity<byte[]> buildPdfResponse(Long id, boolean forDownload) {
        Optional<SolicitudHr> opt = solRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SolicitudHr sol = opt.get();

        try {
            byte[] pdfBytes = loadPdfBytes(sol);

            if (pdfBytes == null || pdfBytes.length == 0) {
                String documentUrl = sol.getDocumentUrl();
                if (documentUrl == null || documentUrl.isBlank())
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
                try (InputStream is = new URL(documentUrl).openStream()) {
                    byte[] excelBytes = is.readAllBytes();
                    String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.xlsx";
                    pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, fileName);
                }
                sol.setPdfUrl("data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes));
                solRepo.save(sol);
            }

            String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.pdf";
            String pdfName  = fileName.replaceAll("\\.[^.]+$", "") + ".pdf";
            ContentDisposition cd = forDownload
                    ? ContentDisposition.attachment().filename(pdfName).build()
                    : ContentDisposition.inline().filename(pdfName).build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(cd);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error al convertir solicitud {} a PDF: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error al convertir el documento: " + e.getMessage()).getBytes());
        }
    }

    // =========================================================================
    // RESPONDER (APROBADO o RECHAZADO)
    // =========================================================================

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToSolicitud(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        String recipientEmail = (String) body.getOrDefault("recipientEmail", "");
        String responseText   = (String) body.getOrDefault("response", "");
        String status         = (String) body.getOrDefault("status", "APROBADO");

        List<SolicitudHrRecipient> recips = recipRepo.findBySolicitudIdAndRecipientEmail(id, recipientEmail);
        if (recips.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se encontró destinatario"));
        }

        SolicitudHrRecipient recip = recips.get(0);
        recip.setStatus(status);
        recip.setResponse(responseText);
        recip.setRespondedAt(LocalDateTime.now());
        recipRepo.save(recip);

        if ("APROBADO".equals(status) || "RECHAZADO".equals(status)) {
            solRepo.findById(id).ifPresent(sol -> {
                try {
                    if (recip.getStampedAt() != null) {
                        log.info("Receptor {} ya fue estampado en solicitud {}, saltando", recipientEmail, id);
                        return;
                    }

                    byte[] pdfBytes = loadPdfBytes(sol);
                    if (pdfBytes == null && sol.getDocumentUrl() != null && !sol.getDocumentUrl().isEmpty()) {
                        try (InputStream is = new URL(sol.getDocumentUrl()).openStream()) {
                            byte[] excelBytes = is.readAllBytes();
                            String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.xlsx";
                            pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, fileName);
                        } catch (Exception ex) {
                            log.warn("Error convirtiendo Excel a PDF en respond: {}", ex.getMessage());
                        }
                    }

                    if (pdfBytes == null || pdfBytes.length == 0) {
                        log.warn("No hay PDF disponible para solicitud {}", id);
                        return;
                    }

                    byte[] resultPdf;

                    if ("RECHAZADO".equals(status)) {
                        // ── RECHAZADO: Toma el cargo del aprobador en vez de su nombre ─────
                        String approverRole = sigRepo.findByEmail(recipientEmail)
                                .map(UserSignature::getRoleLabel)
                                .filter(r -> r != null && !r.isBlank())
                                .orElse(STAFF_ROLES.getOrDefault(recipientEmail, "Aprobador"));

                        resultPdf = pdfService.stampRejected(pdfBytes, responseText, approverRole);
                        log.info("PDF rechazado estampado para solicitud {} por rol {}", id, approverRole);

                    } else {
                        // ── APROBADO: estampar firma en el slot correspondiente ──────
                        Optional<UserSignature> sigOpt = sigRepo.findByEmail(recipientEmail);
                        String sigUrl = sigOpt.map(UserSignature::getSignatureUrl).orElse("");

                        if (sigUrl.isBlank()) {
                            log.warn("Aprobador {} no tiene firma registrada. Solicitud {} marcada sin estampar firma.", recipientEmail, id);
                            recip.setStampedAt(LocalDateTime.now());
                            recipRepo.save(recip);
                            actualizarEstadoGlobal(id);
                            return;
                        }

                        String approverName = sigOpt.map(UserSignature::getName)
                                .orElse(STAFF_DIRECTORY.getOrDefault(recipientEmail, recip.getRecipientName()));
                        String approverRole = STAFF_ROLES.getOrDefault(recipientEmail, "Aprobador");

                        List<PdfService.SigPosition> allPos = pdfService.detectAllSignaturePositions(pdfBytes);
                        log.info("Posiciones detectadas para aprobación solicitud {}: {} posiciones, slotIdx={}",
                                id, allPos.size(), recip.getOrderIndex() + 1);

                        int slotIndex = recip.getOrderIndex() + 1;
                        PdfService.SigPosition pos;

                        if (!allPos.isEmpty()) {
                            if (slotIndex < allPos.size()) {
                                pos = allPos.get(slotIndex);
                            } else {
                                pos = allPos.get(allPos.size() - 1);
                                log.warn("Slot {} no existe, usando último slot disponible ({}) para {}",
                                        slotIndex, allPos.size() - 1, recipientEmail);
                            }
                        } else {
                            float fallbackX = 130f + (160f * slotIndex);
                            pos = new PdfService.SigPosition(fallbackX, 100f, 120f, 35f, 1);
                            log.warn("Sin posiciones detectadas, usando fallback x={} para {}", fallbackX, recipientEmail);
                        }

                        resultPdf = pdfService.stampSignatureAtPosition(pdfBytes, sigUrl,
                                approverName, approverRole,
                                pos.page, pos.x, pos.y, pos.width, pos.height);

                        log.info("Firma de {} estampada en slot {} (x={} y={} pg={}) para solicitud {}",
                                approverName, slotIndex, pos.x, pos.y, pos.page, id);
                    }

                    String stampedB64 = Base64.getEncoder().encodeToString(resultPdf);
                    sol.setPdfUrl("data:application/pdf;base64," + stampedB64);
                    sol.setUpdatedAt(LocalDateTime.now());
                    solRepo.save(sol);

                    recip.setStampedAt(LocalDateTime.now());
                    recipRepo.save(recip);

                } catch (Exception e) {
                    log.error("Error al procesar respuesta para solicitud {} / {}: {}",
                            id, recipientEmail, e.getMessage(), e);
                }
            });
        }

        actualizarEstadoGlobal(id);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "APROBADO".equals(status) ? "Solicitud aprobada correctamente" : "Solicitud rechazada");
        resp.put("status", recip.getStatus());
        return ResponseEntity.ok(resp);
    }

    /** Actualiza el estado global de la solicitud según el estado de todos sus destinatarios. */
    private void actualizarEstadoGlobal(Long id) {
        solRepo.findById(id).ifPresent(sol -> {
            List<SolicitudHrRecipient> allRecips = recipRepo.findBySolicitudIdOrderByCreatedAtDesc(id);
            boolean allDone    = allRecips.stream().allMatch(r ->
                    "APROBADO".equals(r.getStatus()) || "RECHAZADO".equals(r.getStatus()));
            boolean anyRejected = allRecips.stream().anyMatch(r -> "RECHAZADO".equals(r.getStatus()));

            if (allDone) {
                sol.setStatus(anyRejected ? "RECHAZADO" : "PROCESADO");
            } else {
                sol.setStatus("EN_PROCESO");
                for (SolicitudHrRecipient r : allRecips) {
                    if ("EN_ESPERA".equals(r.getStatus())) {
                        boolean prevApproved = allRecips.stream()
                                .filter(p -> p.getOrderIndex() < r.getOrderIndex())
                                .allMatch(p -> "APROBADO".equals(p.getStatus()));
                        if (prevApproved) {
                            r.setStatus("PENDIENTE");
                            recipRepo.save(r);
                        }
                        break;
                    }
                }
            }
            sol.setUpdatedAt(LocalDateTime.now());
            solRepo.save(sol);
        });
    }

    // =========================================================================
    // FIRMA DE USUARIO
    // =========================================================================

    @PostMapping("/signature")
    public ResponseEntity<?> saveSignature(@RequestBody Map<String, Object> body) {
        String email        = (String) body.getOrDefault("email", "");
        String name         = (String) body.getOrDefault("name", "");
        String roleLabel    = (String) body.getOrDefault("roleLabel", "");
        String signatureUrl = (String) body.getOrDefault("signatureUrl", "");

        if (email.isEmpty() || signatureUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email y firma son requeridos"));
        }

        Optional<UserSignature> existing = sigRepo.findByEmail(email);
        UserSignature sig = existing.orElse(new UserSignature());
        sig.setEmail(email);
        sig.setName(name);
        sig.setRoleLabel(roleLabel);
        sig.setSignatureUrl(signatureUrl);
        sig.setUpdatedAt(LocalDateTime.now());
        sigRepo.save(sig);

        return ResponseEntity.ok(Map.of("message", "Firma guardada correctamente"));
    }

    @GetMapping("/signature/{email}")
    public ResponseEntity<?> getSignature(@PathVariable String email) {
        return sigRepo.findByEmail(email)
                .map(sig -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("email",        sig.getEmail());
                    m.put("name",         sig.getName());
                    m.put("roleLabel",    sig.getRoleLabel());
                    m.put("signatureUrl", sig.getSignatureUrl());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.ok(Map.of()));
    }

    // =========================================================================
    // PREVIEW DE POSICIONES
    // =========================================================================

    @GetMapping("/{id}/preview-signatures")
    public ResponseEntity<byte[]> previewSignatures(@PathVariable Long id) {
        Optional<SolicitudHr> opt = solRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        SolicitudHr sol = opt.get();

        try {
            byte[] pdfBytes = loadPdfBytes(sol);
            if (pdfBytes == null && sol.getDocumentUrl() != null && !sol.getDocumentUrl().isEmpty()) {
                try (InputStream is = new URL(sol.getDocumentUrl()).openStream()) {
                    byte[] excelBytes = is.readAllBytes();
                    String fileName = sol.getDocumentName() != null ? sol.getDocumentName() : "documento.xlsx";
                    pdfBytes = excelToPdfService.convertExcelToPdf(excelBytes, fileName);
                }
            }
            if (pdfBytes == null) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            List<PdfService.SigPosition> positions = pdfService.detectAllSignaturePositions(pdfBytes);
            byte[] preview = pdfService.drawSignaturePreview(pdfBytes, positions);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline().filename("preview-signatures.pdf").build());
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            return new ResponseEntity<>(preview, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generando preview de firmas para solicitud {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private byte[] loadPdfBytes(SolicitudHr sol) {
        try {
            if (sol.getPdfUrl() != null && !sol.getPdfUrl().isEmpty()) {
                if (sol.getPdfUrl().startsWith("data:")) {
                    String b64 = sol.getPdfUrl().substring(sol.getPdfUrl().indexOf(",") + 1);
                    return Base64.getDecoder().decode(b64);
                } else {
                    try (InputStream is = new URL(sol.getPdfUrl()).openStream()) {
                        return is.readAllBytes();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error cargando PDF bytes de solicitud {}: {}", sol.getId(), e.getMessage());
        }
        return null;
    }

    private Map<String, Object> toMap(SolicitudHr sol) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           sol.getId());
        m.put("sectionId",    sol.getSectionId());
        m.put("title",        sol.getTitle());
        m.put("description",  sol.getDescription());
        m.put("documentName", sol.getDocumentName());
        m.put("documentUrl",  sol.getDocumentUrl());
        m.put("hasPdf",       sol.getPdfUrl() != null && !sol.getPdfUrl().isEmpty());
        m.put("senderEmail",  sol.getSenderEmail());
        m.put("senderName",   sol.getSenderName());
        m.put("status",       sol.getStatus());
        m.put("createdAt",    sol.getCreatedAt() != null ? sol.getCreatedAt().toString() : null);
        m.put("updatedAt",    sol.getUpdatedAt() != null ? sol.getUpdatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toDetailMap(SolicitudHr sol) {
        Map<String, Object> m = toMap(sol);
        m.put("pdfUrl", sol.getPdfUrl());
        return m;
    }

    private Map<String, Object> toRecipMap(SolicitudHrRecipient r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             r.getId());
        m.put("recipientEmail", r.getRecipientEmail());
        m.put("recipientName",  r.getRecipientName());
        m.put("status",         r.getStatus());
        m.put("response",       r.getResponse());
        m.put("orderIndex",     r.getOrderIndex());
        m.put("respondedAt",    r.getRespondedAt() != null ? r.getRespondedAt().toString() : null);
        m.put("createdAt",      r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }
}