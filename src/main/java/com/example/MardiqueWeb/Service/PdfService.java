package com.example.MardiqueWeb.Service;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private static final float SIG_IMG_MAX_W       = 90f;
    private static final float SIG_IMG_MAX_H       = 32f;
    private static final float SIG_IMG_ABOVE_LABEL = 10f;

    private static final int IMAGE_MAX_BYTES    = 200 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS    = 5_000;

    public static class SigPosition {
        public final float x, y, width, height;
        public final int   page;
        public SigPosition(float x, float y, float width, float height, int page) {
            this.x = x; this.y = y; this.width = width; this.height = height; this.page = page;
        }
        @Override public String toString() {
            return String.format("SigPos(x=%.0f y=%.0f page=%d)", x, y, page);
        }
    }

    private static class FoundText {
        final String text;
        final float  x, y, width, height, pageHeight;
        final int    page;
        FoundText(String t, float x, float y, float w, float h, int pg, float pH) {
            this.text = t; this.x = x; this.y = y;
            this.width = w; this.height = h; this.page = pg; this.pageHeight = pH;
        }
        float bottomUpY() { return pageHeight - y; }
        float centerX()   { return x + width / 2f; }
    }

    // =========================================================================
    // 1. DETECCIÓN DE POSICIONES DE FIRMA
    // =========================================================================

    public List<SigPosition> detectAllSignaturePositions(byte[] pdfBytes) {
        try {
            List<FoundText> all = scanTextPositions(pdfBytes);
            if (all.isEmpty()) return fallbackPositions(1);

            List<FoundText> candidates = all.stream()
                    .filter(this::esSigLabel)
                    .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                int lp = all.stream().mapToInt(f -> f.page).max().orElse(1);
                return fallbackPositions(lp);
            }

            int lastPage = candidates.stream().mapToInt(f -> f.page).max().orElse(1);
            List<FoundText> pool = candidates.stream()
                    .filter(f -> f.page == lastPage)
                    .collect(Collectors.toList());
            if (pool.isEmpty()) pool = candidates;

            List<FoundText> lower = pool.stream()
                    .filter(f -> f.bottomUpY() < f.pageHeight * 0.55f)
                    .collect(Collectors.toList());
            if (lower.isEmpty()) lower = pool;

            TextLine solicitante = null;
            TextLine jefe        = null;
            TextLine gerente     = null;
            TextLine talento     = null;

            List<List<FoundText>> clusters = clusterByProximity(lower, 80f, 20f);

            for (List<FoundText> cluster : clusters) {
                TextLine tl = buildLine(cluster);
                String t = tl.fullText.toLowerCase().trim();

                if (matches(t, "talento humano", "vobo talento", "vbo talento", "talento")) {
                    if (talento == null || tl.y < talento.y) talento = tl;
                } else if (matches(t, "firma gerente", "gerencia", "gerente", "vbo gerencia", "v.b.o gerencia", "visto bueno gerencia")) {
                    if (gerente == null || tl.y < gerente.y) gerente = tl;
                } else if (matches(t, "jefe inmediato", "firma jefe", "jefe", "inmediato", "coordinador")) {
                    if (jefe == null || tl.y < jefe.y) jefe = tl;
                } else if (matches(t, "firma solicitante", "solicitante", "empleado", "trabajador")) {
                    if (solicitante == null || tl.y < solicitante.y) solicitante = tl;
                }
            }

            List<SigPosition> positions = new ArrayList<>();
            if (solicitante != null) positions.add(toPos(solicitante));
            if (jefe != null) positions.add(toPos(jefe));
            if (gerente != null) positions.add(toPos(gerente));
            if (talento != null) positions.add(toPos(talento));

            if (positions.isEmpty()) {
                return fallbackPositions(lastPage);
            }

            return positions;

        } catch (Exception e) {
            log.error("Error en detectAllSignaturePositions: {}", e.getMessage(), e);
            return fallbackPositions(1);
        }
    }

    private boolean esSigLabel(FoundText ft) {
        String s = ft.text.toLowerCase().trim();
        if (s.length() < 3 || s.length() > 60) return false;
        return s.contains("firma") || s.contains("empleado") || s.contains("jefe")
                || s.contains("inmediato") || s.contains("solicitante") || s.contains("gerente")
                || s.contains("gerencia") || s.contains("talento") || s.contains("humano")
                || s.equals("vbo") || s.contains("v.b.o") || s.contains("vobo")
                || s.contains("autoriza") || s.contains("aprobaci");
    }

    private List<SigPosition> fallbackPositions(int page) {
        return List.of(
                new SigPosition(130, 200, SIG_IMG_MAX_W, SIG_IMG_MAX_H, page),
                new SigPosition(310, 200, SIG_IMG_MAX_W, SIG_IMG_MAX_H, page),
                new SigPosition(490, 200, SIG_IMG_MAX_W, SIG_IMG_MAX_H, page)
        );
    }

    private static class TextLine {
        final String fullText;
        final double x;
        final float y;
        final int page;
        final float pageHeight;
        TextLine(String fullText, double x, float y, int page, float pageHeight) {
            this.fullText = fullText;
            this.x = x;
            this.y = y;
            this.page = page;
            this.pageHeight = pageHeight;
        }
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private SigPosition toPos(TextLine tl) {
        return new SigPosition((float) tl.x, tl.y, SIG_IMG_MAX_W, SIG_IMG_MAX_H, tl.page);
    }

    private List<List<FoundText>> clusterByProximity(List<FoundText> items, float xTol, float yTol) {
        List<List<FoundText>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[items.size()];
        for (int i = 0; i < items.size(); i++) {
            if (assigned[i]) continue;
            List<FoundText> cluster = new ArrayList<>();
            cluster.add(items.get(i));
            assigned[i] = true;
            for (int j = i + 1; j < items.size(); j++) {
                if (assigned[j]) continue;
                FoundText a = items.get(i), b = items.get(j);
                if (Math.abs(a.x - b.x) <= xTol && Math.abs(a.bottomUpY() - b.bottomUpY()) <= yTol) {
                    cluster.add(b);
                    assigned[j] = true;
                }
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private TextLine buildLine(List<FoundText> items) {
        StringBuilder sb = new StringBuilder();
        float minX = Float.MAX_VALUE, maxX = 0;
        float minY = Float.MAX_VALUE;
        int page = items.get(0).page;
        float pH = items.get(0).pageHeight;
        for (FoundText ft : items) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(ft.text);
            if (ft.x < minX) minX = ft.x;
            float right = ft.x + ft.width;
            if (right > maxX) maxX = right;
            if (ft.y < minY) minY = ft.y;
        }
        double centerX = (minX + maxX) / 2.0;
        return new TextLine(sb.toString(), centerX, pH - minY, page, pH);
    }

    // =========================================================================
    // 2. SCAN DE TEXTO
    // =========================================================================

    private List<FoundText> scanTextPositions(byte[] pdfBytes) throws Exception {
        List<FoundText> results = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                final int   pageNum    = p;
                final float pageHeight = doc.getPage(p).getMediaBox().getHeight();

                PDFTextStripper stripper = new PDFTextStripper() {
                    @Override
                    protected void writeString(String text, List<TextPosition> positions) {
                        if (positions == null || positions.isEmpty()) return;
                        StringBuilder word = new StringBuilder();
                        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                        float maxX = 0, maxY = 0;
                        Float lastEndX = null;
                        for (TextPosition tp : positions) {
                            float tx = tp.getXDirAdj(), ty = tp.getYDirAdj(), tw = tp.getWidthDirAdj();
                            if (lastEndX != null && tx - lastEndX > 15f) {
                                flush(word, minX, minY, maxX, maxY, results);
                                word.setLength(0);
                                minX = Float.MAX_VALUE; minY = Float.MAX_VALUE; maxX = 0; maxY = 0;
                            }
                            word.append(tp.getUnicode());
                            if (tx < minX)      minX = tx;
                            if (ty < minY)      minY = ty;
                            if (tx+tw > maxX)   maxX = tx+tw;
                            if (ty > maxY)      maxY = ty;
                            lastEndX = tx + tw;
                        }
                        flush(word, minX, minY, maxX, maxY, results);
                    }
                    private void flush(StringBuilder sb, float x1, float y1, float x2, float y2,
                                       List<FoundText> out) {
                        String s = sb.toString().trim();
                        if (!s.isEmpty())
                            out.add(new FoundText(s, x1, y1, x2-x1, y2-y1, pageNum+1, pageHeight));
                    }
                };
                stripper.setSortByPosition(true);
                stripper.setStartPage(p + 1);
                stripper.setEndPage(p + 1);
                stripper.getText(doc);
            }
        }
        return results;
    }

    // =========================================================================
    // 3. ESTAMPAR FIRMA
    // =========================================================================

    public byte[] stampSignatureAtPosition(byte[] pdfBytes,
                                           String signatureImageUrl,
                                           String approverName,
                                           String approverRole,
                                           int    pageIndex,
                                           float  centerX,
                                           float  yPt,
                                           float  widthPt,
                                           float  heightPt) {
        if (pdfBytes == null || pdfBytes.length == 0) return pdfBytes;

        PdfReader  reader  = null;
        PdfStamper stamper = null;
        try (ByteArrayInputStream  bais = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            reader  = new PdfReader(bais);
            int totalPages = reader.getNumberOfPages();
            int page = (pageIndex <= 0 || pageIndex > totalPages) ? totalPages : pageIndex;

            stamper = new PdfStamper(reader, baos);
            PdfContentByte over = stamper.getOverContent(page);

            float imgBottomY = yPt + SIG_IMG_ABOVE_LABEL;

            if (signatureImageUrl != null && !signatureImageUrl.isBlank()) {
                byte[] imgBytes = downloadImageSafe(signatureImageUrl);
                if (imgBytes != null && imgBytes.length > 0) {
                    Image sig = Image.getInstance(imgBytes);
                    sig.scaleToFit(SIG_IMG_MAX_W, SIG_IMG_MAX_H);
                    float imgW = sig.getScaledWidth();
                    sig.setAbsolutePosition(centerX - imgW / 2f, imgBottomY);
                    over.addImage(sig);
                }
            }

            stamper.close(); stamper = null;
            reader.close();  reader  = null;
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error estampando firma: {}", e.getMessage(), e);
            return pdfBytes;
        } finally {
            if (stamper != null) { try { stamper.close(); } catch (Exception ignored) {} }
            if (reader  != null) { try { reader.close();  } catch (Exception ignored) {} }
        }
    }

    public String stampSignatureAtPositionAndUpload(byte[] pdfBytes, String signatureImageUrl,
                                                    String approverName, String approverRole,
                                                    int pageIndex,
                                                    float xPt, float yPt, float w, float h) {
        byte[] stamped = stampSignatureAtPosition(pdfBytes, signatureImageUrl,
                approverName, approverRole, pageIndex, xPt, yPt, w, h);
        return Base64.getEncoder().encodeToString(stamped);
    }

    // =========================================================================
    // 4. ESTAMPAR RECHAZO (REFINADO: Cargo, Tamaño 11, Izquierda)
    // =========================================================================

    public byte[] stampRejected(byte[] pdfBytes, String comment, String rejectorRole) {
        if (pdfBytes == null || pdfBytes.length == 0) return pdfBytes;

        PdfReader  reader  = null;
        PdfStamper stamper = null;
        try (ByteArrayInputStream  bais = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            reader  = new PdfReader(bais);
            stamper = new PdfStamper(reader, baos);
            int pageCount = reader.getNumberOfPages();

            // Construye el texto usando el Cargo y el Comentario
            String obsText = buildObsText(comment, rejectorRole);

            for (int p = 1; p <= pageCount; p++) {
                com.lowagie.text.Rectangle ps = reader.getPageSizeWithRotation(p);
                float w = ps.getWidth(), h = ps.getHeight();
                PdfContentByte over = stamper.getOverContent(p);

                // Marca de agua de RECHAZADO (Translúcida y limpia)
                over.saveState();
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.06f);
                gs.setStrokeOpacity(0.06f);
                over.setGState(gs);
                BaseFont wmBf = loadCalibriBold();
                over.beginText();
                over.setFontAndSize(wmBf, 90);
                over.setColorFill(Color.RED);
                over.showTextAligned(PdfContentByte.ALIGN_CENTER, "RECHAZADO", w / 2f, h / 2f, 45);
                over.endText();
                over.restoreState();

                // Colocar la observación (Cargo + Comentario)
                if (p == pageCount && obsText != null && !obsText.isBlank()) {
                    float obsY = findObservacionesY(pdfBytes, p);

                    over.saveState();
                    BaseFont obsBf = loadCalibriRegular();
                    over.beginText();

                    // Requisito exacto: Tamaño 11 y color negro puro
                    over.setFontAndSize(obsBf, 11);
                    over.setColorFill(Color.BLACK);

                    String display = obsText.length() > 220 ? obsText.substring(0, 217) + "..." : obsText;

                    // ALIGN_LEFT: A la izquierda.
                    // X = 42f (Poco después del margen, para alinear debajo de 'Observaciones:')
                    // Y = obsY - 18f (Baja exactamente un renglón debajo del título 'Observaciones:')
                    over.showTextAligned(PdfContentByte.ALIGN_LEFT, display, 42f, obsY - 18f, 0);

                    over.endText();
                    over.restoreState();
                }
            }

            stamper.close(); stamper = null;
            reader.close();  reader  = null;
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error estampando rechazo: {}", e.getMessage(), e);
            return pdfBytes;
        } finally {
            if (stamper != null) { try { stamper.close(); } catch (Exception ignored) {} }
            if (reader  != null) { try { reader.close();  } catch (Exception ignored) {} }
        }
    }

    private String buildObsText(String comment, String rejectorRole) {
        StringBuilder sb = new StringBuilder();
        if (rejectorRole != null && !rejectorRole.isBlank()) sb.append(rejectorRole).append(": ");
        sb.append(comment != null && !comment.isBlank() ? comment : "Rechazado sin comentario.");
        return sb.toString();
    }

    private float findObservacionesY(byte[] pdfBytes, int targetPage) {
        try {
            for (FoundText ft : scanTextPositions(pdfBytes))
                if (ft.page == targetPage && ft.text.toLowerCase().contains("observaci"))
                    return ft.bottomUpY(); // Toma exactamente la línea base de la palabra "Observaciones:"
        } catch (Exception e) {
            log.warn("No se detectó Observaciones: {}", e.getMessage());
        }
        return 120f;
    }

    // =========================================================================
    // 5. PREVIEW
    // =========================================================================

    public byte[] drawSignaturePreview(byte[] pdfBytes, List<SigPosition> positions) {
        PdfReader  reader  = null;
        PdfStamper stamper = null;
        try (ByteArrayInputStream  bais = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            reader  = new PdfReader(bais);
            stamper = new PdfStamper(reader, baos);
            for (int i = 0; i < positions.size(); i++) {
                SigPosition pos = positions.get(i);
                int page = Math.min(Math.max(pos.page <= 0 ? reader.getNumberOfPages() : pos.page,
                        1), reader.getNumberOfPages());
                PdfContentByte over = stamper.getOverContent(page);
                float px = pos.x - pos.width/2f, py = pos.y;
                over.saveState();
                over.setColorFill(new Color(0,150,255,40));
                over.setColorStroke(new Color(0,100,255,200));
                over.setLineWidth(1f);
                over.rectangle(px, py, pos.width, pos.height);
                over.fillStroke();
                over.restoreState();
                BaseFont lf = loadCalibriRegular();
                over.beginText();
                over.setFontAndSize(lf, 8);
                over.setColorFill(new Color(0,100,255));
                over.showTextAligned(PdfContentByte.ALIGN_CENTER,
                        "#"+(i+1)+" p"+pos.page, pos.x, py+pos.height+4, 0);
                over.endText();
            }
            stamper.close(); stamper = null;
            reader.close();  reader  = null;
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error en preview: {}", e.getMessage(), e);
            return pdfBytes;
        } finally {
            if (stamper != null) { try { stamper.close(); } catch (Exception ignored) {} }
            if (reader  != null) { try { reader.close();  } catch (Exception ignored) {} }
        }
    }

    // =========================================================================
    // 6. FUENTES
    // =========================================================================

    private BaseFont loadCalibriBold() {
        return tryLoadFont(new String[]{
                "C:/Windows/Fonts/calibrib.ttf",
                "/usr/share/fonts/truetype/msttcorefonts/calibrib.ttf",
                "/usr/share/fonts/truetype/calibri/calibrib.ttf",
                "/usr/share/fonts/calibrib.ttf",
                "/fonts/calibrib.ttf"
        }, BaseFont.HELVETICA_BOLD);
    }

    private BaseFont loadCalibriRegular() {
        return tryLoadFont(new String[]{
                "C:/Windows/Fonts/calibri.ttf",
                "/usr/share/fonts/truetype/msttcorefonts/calibri.ttf",
                "/usr/share/fonts/truetype/calibri/calibri.ttf",
                "/usr/share/fonts/calibri.ttf",
                "/fonts/calibri.ttf"
        }, BaseFont.HELVETICA);
    }

    private BaseFont tryLoadFont(String[] paths, String fallback) {
        for (String path : paths) {
            try {
                if (new java.io.File(path).exists())
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }
        try { return BaseFont.createFont(fallback, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); }
        catch (Exception e) {
            try { return BaseFont.createFont(); }
            catch (Exception ex) { throw new RuntimeException("No se pudo cargar fuente", ex); }
        }
    }

    // =========================================================================
    // 7. DESCARGA DE IMAGEN
    // =========================================================================

    private byte[] downloadImageSafe(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.warn("HTTP {} descargando firma: {}", conn.getResponseCode(), urlStr);
                return null;
            }
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream b = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096]; int r, total = 0;
                while ((r = in.read(buf)) != -1) {
                    total += r;
                    if (total > IMAGE_MAX_BYTES) { log.warn("Imagen supera límite"); return null; }
                    b.write(buf, 0, r);
                }
                return b.toByteArray();
            }
        } catch (Exception e) {
            log.warn("Error descargando imagen: {}", e.getMessage());
            return null;
        }
    }

    public byte[] excelToPdf(byte[] excelBytes, String fileName) {
        try (InputStream is = new ByteArrayInputStream(excelBytes);
             XSSFWorkbook wb = new XSSFWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Documento: " + fileName,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error convirtiendo Excel: {}", e.getMessage(), e);
            return new byte[0];
        }
    }
}