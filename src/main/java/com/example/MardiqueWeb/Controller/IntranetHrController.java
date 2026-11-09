package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetHrDocument;
import com.example.MardiqueWeb.Entity.IntranetHrSectionView;
import com.example.MardiqueWeb.Repository.IntranetHrDocumentRepository;
import com.example.MardiqueWeb.Repository.IntranetHrSectionViewRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.example.MardiqueWeb.Service.HrFormatPdfService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/intranet/hr")
public class IntranetHrController {

    private static final Logger log = LoggerFactory.getLogger(IntranetHrController.class);
    private static final Map<Long, byte[]> THUMB_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final Set<String> EXCEL_TYPES = Set.of("xls", "xlsx", "csv");
    private static final Set<String> WORD_TYPES = Set.of("doc", "docx");
    private static final int MAX_SHEET_ROWS = 40;
    private static final int MAX_SHEET_COLS = 26;
    private final RestTemplate restTemplate = new RestTemplate();

    private final IntranetHrSectionViewRepository viewRepo;
    private final IntranetHrDocumentRepository docRepo;
    private final HrFormatPdfService hrFormatPdfService;

    public IntranetHrController(IntranetHrSectionViewRepository viewRepo,
                                IntranetHrDocumentRepository docRepo,
                                HrFormatPdfService hrFormatPdfService) {
        this.viewRepo = viewRepo;
        this.docRepo = docRepo;
        this.hrFormatPdfService = hrFormatPdfService;
    }

    public static final Map<String, String> SECTION_NAMES = Map.of(
        "reglamento", "Reglamento Interno de Trabajo",
        "permisos", "Solicitud de Permiso",
        "vacaciones", "Solicitud de Vacaciones",
        "compensacion-vacaciones", "Formato de Compensación de Vacaciones",
        "retiro-cesantias", "Formato Retiro Parcial de Cesantías",
        "reembolso-gastos", "Formato de Reembolso de Gastos",
        "dia-familia", "Carta Solicitud Día de la Familia",
        "horas-extras", "Formato de Horas Extras y Recargos"
    );

    /* ── QR ── */

    @GetMapping("/{sectionId}/qr")
    public ResponseEntity<byte[]> sectionQr(@PathVariable String sectionId,
                                            jakarta.servlet.http.HttpServletRequest req) {
        String scheme = req.getScheme();
        String host = req.getServerName();
        int port = req.getServerPort();
        String ctx = req.getContextPath();
        String portStr = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80) ? "" : ":" + port;
        String url = scheme + "://" + host + portStr + ctx + "/intranet/autoservicio-th#" + sectionId;
        byte[] png = generateQrPng(url);
        if (png == null) return ResponseEntity.status(500).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDisposition(ContentDisposition.inline().filename("qr-" + sectionId + ".png").build());
        headers.setCacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(10)).getHeaderValue());
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }

    /* ── PDF Format download ── */

    @GetMapping("/{sectionId}/format")
    public ResponseEntity<byte[]> downloadFormat(@PathVariable String sectionId) {
        byte[] pdf = hrFormatPdfService.generateFormat(sectionId);
        if (pdf == null || pdf.length == 0) return ResponseEntity.notFound().build();
        String name = SECTION_NAMES.getOrDefault(sectionId, sectionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("FT-GATH-SPM-" + name.replaceAll("[^a-zA-Z0-9]", "-") + ".pdf").build());
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /* ── Views tracking ── */

    @PostMapping("/{sectionId}/view")
    public ResponseEntity<?> recordView(@PathVariable String sectionId,
                                        @RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String name = body.getOrDefault("name", "").trim();
        if (email.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "email required"));
        IntranetHrSectionView view = new IntranetHrSectionView();
        view.setSectionId(sectionId);
        view.setViewerEmail(email);
        view.setViewerName(name.isEmpty() ? null : name);
        view.setViewedAt(LocalDateTime.now());
        viewRepo.save(view);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{sectionId}/views")
    public ResponseEntity<?> getViews(@PathVariable String sectionId) {
        List<IntranetHrSectionView> views = viewRepo.findBySectionIdOrderByViewedAtDesc(sectionId);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (IntranetHrSectionView v : views) {
            String key = v.getViewerEmail();
            grouped.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", v.getViewerEmail());
                m.put("name", v.getViewerName());
                m.put("viewCount", 0);
                m.put("lastViewedAt", v.getViewedAt().toString());
                m.put("viewedAt", v.getViewedAt().toString());
                return m;
            });
            Map<String, Object> m = grouped.get(key);
            m.put("viewCount", (int) m.get("viewCount") + 1);
            m.put("lastViewedAt", v.getViewedAt().toString());
        }
        return ResponseEntity.ok(new ArrayList<>(grouped.values()));
    }

    @GetMapping("/views/events")
    public ResponseEntity<?> allEvents() {
        return ResponseEntity.ok(viewRepo.findAll());
    }

    @GetMapping("/views/stats")
    public ResponseEntity<?> stats() {
        List<IntranetHrSectionView> all = viewRepo.findAll();
        Map<String, Long> bySection = all.stream()
            .collect(Collectors.groupingBy(IntranetHrSectionView::getSectionId, Collectors.counting()));
        long totalViews = all.size();
        Set<String> uniqueEmails = all.stream().map(IntranetHrSectionView::getViewerEmail).collect(Collectors.toSet());
        return ResponseEntity.ok(Map.of(
            "totalViews", totalViews,
            "uniqueUsers", uniqueEmails.size(),
            "bySection", bySection
        ));
    }

    /* ── Documents CRUD ── */

    @GetMapping("/{sectionId}/docs")
    public ResponseEntity<?> listDocs(@PathVariable String sectionId) {
        return ResponseEntity.ok(docRepo.findBySectionIdOrderByUploadedAtDesc(sectionId));
    }

    @PostMapping("/{sectionId}/docs")
    public ResponseEntity<?> addDoc(@PathVariable String sectionId, @RequestBody Map<String, String> body) {
        String fileName = body.getOrDefault("fileName", "").trim();
        String fileUrl = body.getOrDefault("fileUrl", "").trim();
        String description = body.getOrDefault("description", "").trim();
        String uploadedBy = body.getOrDefault("uploadedBy", "").trim();
        if (fileName.isEmpty() || fileUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "fileName and fileUrl required"));
        }
        IntranetHrDocument doc = new IntranetHrDocument();
        doc.setSectionId(sectionId);
        doc.setFileName(fileName);
        doc.setFileUrl(fileUrl);
        doc.setDescription(description.isEmpty() ? null : description);
        doc.setUploadedBy(uploadedBy.isEmpty() ? null : uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());
        docRepo.save(doc);
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/docs/{docId}")
    public ResponseEntity<?> deleteDoc(@PathVariable Long docId) {
        docRepo.deleteById(docId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /* ── Thumbnail ── */

    @GetMapping("/docs/{docId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long docId) {
        try {
            Optional<IntranetHrDocument> opt = docRepo.findById(docId);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            IntranetHrDocument doc = opt.get();
            byte[] cached = THUMB_CACHE.get(docId);
            if (cached != null) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(cached);
            }
            String ext = getExt(doc.getFileName());
            byte[] png;
            if ("pdf".equals(ext)) {
                png = pdfThumb(doc.getFileUrl());
            } else if (IMAGE_TYPES.contains(ext)) {
                png = imageThumb(doc.getFileUrl());
            } else if (EXCEL_TYPES.contains(ext)) {
                png = excelThumb(doc.getFileUrl(), ext);
            } else if (WORD_TYPES.contains(ext)) {
                png = wordThumb(doc.getFileUrl(), ext);
            } else {
                png = genericThumb(ext);
            }
            if (png != null) THUMB_CACHE.put(docId, png);
            if (png == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
        } catch (Exception e) {
            log.error("HR thumbnail error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    private String getExt(String fileName) {
        if (fileName == null) return "";
        int i = fileName.lastIndexOf('.');
        return i >= 0 ? fileName.substring(i + 1).toLowerCase() : "";
    }

    private byte[] fetchBytes(String url) {
        return restTemplate.getForObject(url, byte[].class);
    }

    private byte[] pdfThumb(String fileUrl) {
        try {
            byte[] pdfBytes = fetchBytes(fileUrl);
            if (pdfBytes == null) return genericThumb("pdf");
            try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                if (pdf.getNumberOfPages() == 0) return genericThumb("pdf");
                PDFRenderer renderer = new PDFRenderer(pdf);
                BufferedImage page = renderer.renderImageWithDPI(0, 110);
                return toPng(scaleToFit(page, 420, 560));
            }
        } catch (Exception e) {
            log.warn("PDF thumb failed, fallback generic: {}", e.getMessage());
            return genericThumb("pdf");
        }
    }

    private byte[] imageThumb(String fileUrl) {
        try {
            byte[] imgBytes = fetchBytes(fileUrl);
            if (imgBytes == null) return genericThumb("img");
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(imgBytes));
            if (img == null) return genericThumb("img");
            return toPng(scaleToFit(img, 420, 560));
        } catch (Exception e) {
            return genericThumb("img");
        }
    }

    private byte[] wordThumb(String fileUrl, String ext) {
        try {
            byte[] data = fetchBytes(fileUrl);
            if (data == null) return genericThumb(ext);
            byte[] viaLO = libreOfficeThumb(data, ext);
            return viaLO != null ? viaLO : genericThumb(ext);
        } catch (Exception e) {
            log.warn("Word thumb failed: {}", e.getMessage());
            return genericThumb(ext);
        }
    }

    private String resolveLibreOffice() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] c = {"C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                    "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"};
            for (String p : c) if (new java.io.File(p).exists()) return p;
            return null;
        } else {
            String[] c = {"/usr/bin/libreoffice", "/usr/bin/soffice", "/opt/libreoffice/program/soffice"};
            for (String p : c) if (new java.io.File(p).exists()) return p;
            return null;
        }
    }

    private byte[] excelThumb(String fileUrl, String ext) {
        try {
            byte[] data = fetchBytes(fileUrl);
            if (data == null) return genericThumb(ext);
            byte[] viaLO = libreOfficeThumb(data, ext);
            if (viaLO != null) return viaLO;
            String[][] grid = parseSpreadsheet(data, ext);
            if (grid == null) return genericThumb(ext);
            return toPng(drawSheetGrid(grid, ext));
        } catch (Exception e) {
            return genericThumb(ext);
        }
    }

    private byte[] libreOfficeThumb(byte[] data, String ext) {
        try {
            String loBin = resolveLibreOffice();
            if (loBin == null) return null;
            Path tmpDir = Files.createTempDirectory("lo_thumb_" + System.nanoTime());
            try {
                String inExt = ext.startsWith("doc") ? ("docx".equals(ext) ? ".docx" : ".doc") : ("xlsx".equals(ext) ? ".xlsx" : ".xls");
                Path inputFile = tmpDir.resolve("doc" + inExt);
                Files.write(inputFile, data);
                ProcessBuilder pb = new ProcessBuilder(loBin, "--headless", "--norestore",
                        "--nofirststartwizard", "--convert-to", "pdf",
                        "--outdir", tmpDir.toString(), inputFile.toAbsolutePath().toString());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                boolean done = proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                if (!done) { proc.destroyForcibly(); return null; }
                Path pdfOut = tmpDir.resolve("doc.pdf");
                if (!Files.exists(pdfOut)) {
                    var found = Files.list(tmpDir).filter(p -> p.toString().endsWith(".pdf")).findFirst();
                    if (found.isEmpty()) return null;
                    pdfOut = found.get();
                }
                byte[] pdfBytes = Files.readAllBytes(pdfOut);
                try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
                    if (pdf.getNumberOfPages() == 0) return null;
                    PDFRenderer renderer = new PDFRenderer(pdf);
                    BufferedImage page = renderer.renderImageWithDPI(0, 110);
                    return toPng(scaleToFit(page, 420, 560));
                }
            } finally {
                Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile).forEach(java.io.File::delete);
            }
        } catch (Exception e) {
            log.warn("LibreOffice thumb failed for {}: {}", ext, e.getMessage());
            return null;
        }
    }

    private String[][] parseSpreadsheet(byte[] data, String ext) {
        try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            String[][] grid = new String[MAX_SHEET_ROWS][MAX_SHEET_COLS];
            for (String[] row : grid) Arrays.fill(row, "");
            int maxRow = Math.min(sheet.getLastRowNum() + 1, MAX_SHEET_ROWS);
            for (int r = 0; r < maxRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int last = Math.min(Math.max(row.getLastCellNum(), 0), MAX_SHEET_COLS);
                for (int col = 0; col < last; col++) {
                    Cell cell = row.getCell(col);
                    if (cell != null) grid[r][col] = fmt.formatCellValue(cell);
                }
            }
            return grid;
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage drawSheetGrid(String[][] grid, String ext) {
        int w = 520, h = 680;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            Color excelGreen = new Color(33, 115, 70);
            Color headerBg = new Color(226, 235, 245);
            Color rowNumBg = new Color(245, 247, 250);
            Color rowNumFg = new Color(120, 130, 145);
            Color gridLine = new Color(218, 223, 230);
            Color cellText = new Color(30, 41, 59);
            Color headerText = new Color(50, 60, 75);
            Color stripeBg = new Color(250, 252, 254);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);

            int rowNumberW = 36;
            int formulaBarH = 28;
            int colHeaderH = 22;
            int rowH = 20;
            int left = rowNumberW;
            int top = formulaBarH;

            g.setColor(excelGreen);
            g.fillRect(0, 0, w, 6);

            g.setColor(new Color(248, 249, 251));
            g.fillRect(0, 6, w, formulaBarH - 6);
            g.setColor(gridLine);
            g.drawLine(0, formulaBarH - 1, w, formulaBarH - 1);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            g.setColor(new Color(100, 116, 139));
            g.drawString("fx", 8, formulaBarH - 8);
            g.setColor(gridLine);
            g.drawLine(24, 8, 24, formulaBarH - 1);

            int usedCols = 1;
            for (int colC = MAX_SHEET_COLS - 1; colC >= 0; colC--) {
                for (int r = 0; r < grid.length; r++) {
                    if (!grid[r][colC].isEmpty()) { usedCols = colC + 1; break; }
                }
                if (usedCols > 1) break;
            }
            if (usedCols < 1) usedCols = 1;
            int dataW = w - left - 8;
            int colW = Math.max(dataW / usedCols, 48);

            g.setColor(headerBg);
            g.fillRect(left, top, w - left, colHeaderH);
            g.setColor(gridLine);
            g.drawLine(0, top + colHeaderH, w, top + colHeaderH);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            for (int col = 0; col < usedCols; col++) {
                int x = left + col * colW;
                if (x + colW > w) break;
                g.setColor(headerText);
                String label = String.valueOf((char) ('A' + col));
                FontMetrics fm = g.getFontMetrics();
                int lw = fm.stringWidth(label);
                g.drawString(label, x + (colW - lw) / 2, top + colHeaderH - 7);
                g.setColor(gridLine);
                g.drawLine(x, top, x, top + colHeaderH);
                g.drawLine(x + colW, top, x + colW, top + colHeaderH);
            }

            int y = top + colHeaderH;
            int maxRows = Math.min(grid.length, (h - y - 10) / rowH);

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            for (int r = 0; r < maxRows; r++) {
                g.setColor(rowNumBg);
                g.fillRect(0, y, rowNumberW, rowH);
                g.setColor(rowNumFg);
                FontMetrics rfm = g.getFontMetrics();
                String rn = String.valueOf(r + 1);
                g.drawString(rn, rowNumberW - rfm.stringWidth(rn) - 6, y + rowH - 6);
                g.setColor(gridLine);
                g.drawLine(0, y, w, y);
                g.drawLine(rowNumberW, y, rowNumberW, y + rowH);

                if (r % 2 == 1) {
                    g.setColor(stripeBg);
                    g.fillRect(left, y, w - left, rowH);
                }

                for (int col = 0; col < usedCols; col++) {
                    int x = left + col * colW;
                    if (x + colW > w) break;
                    g.setColor(gridLine);
                    g.drawLine(x + colW, y, x + colW, y + rowH);

                    String cell = grid[r][col];
                    if (!cell.isEmpty()) {
                        g.setColor(cellText);
                        String draw = cell.length() > 18 ? cell.substring(0, 17) + "…" : cell;
                        g.drawString(draw, x + 5, y + rowH - 6);
                    }
                }
            }
            g.setColor(gridLine);
            g.drawLine(0, y, w, y);

            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, h - 24, w, 24);
            g.setColor(gridLine);
            g.drawLine(0, h - 24, w, h - 24);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
            g.setColor(rowNumFg);
            g.drawString("Hoja1", 8, h - 8);
            g.setColor(new Color(200, 205, 215));
            g.fillOval(w - 20, h - 18, 10, 10);

        } finally {
            g.dispose();
        }
        return img;
    }

    private byte[] genericThumb(String ext) {
        int w = 420, h = 560;
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            Color c = typeColor(ext);
            g.setColor(c);
            g.fillRect(0, 0, w, 14);
            g.fillRoundRect(50, 80, w - 100, 280, 16, 16);
            g.dispose();
            return toPng(img);
        } catch (Exception e) {
            return null;
        }
    }

    private Color typeColor(String ext) {
        if (ext == null) return new Color(148, 163, 184);
        return switch (ext.toLowerCase()) {
            case "pdf" -> new Color(220, 38, 38);
            case "doc", "docx" -> new Color(37, 99, 235);
            case "xls", "xlsx", "csv" -> new Color(22, 163, 74);
            case "jpg", "jpeg", "png", "gif", "webp", "svg" -> new Color(168, 85, 247);
            default -> new Color(148, 163, 184);
        };
    }

    private BufferedImage scaleToFit(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
        int nw = (int) (src.getWidth() * scale);
        int nh = (int) (src.getHeight() * scale);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    /* ── QR generation ── */

    private byte[] generateQrPng(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(com.google.zxing.EncodeHintType.MARGIN, 2);
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(url,
                    com.google.zxing.BarcodeFormat.QR_CODE, 600, 600, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int scale = 4;
            BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                g.setColor(new Color(2, 6, 23));
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        if (matrix.get(x, y)) {
                            g.fillRect(x * scale, y * scale, scale, scale);
                        }
                    }
                }
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
