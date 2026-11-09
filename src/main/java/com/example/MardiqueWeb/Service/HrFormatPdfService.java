package com.example.MardiqueWeb.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Service
public class HrFormatPdfService {

    private static final Logger log = LoggerFactory.getLogger(HrFormatPdfService.class);

    private static final Color NAVY = new Color(27, 69, 98);
    private static final Color GOLD = new Color(240, 154, 54);
    private static final Color GRAY = new Color(100, 116, 139);
    private static final Color LIGHT_GRAY = new Color(226, 232, 240);
    private static final Color VERY_LIGHT = new Color(248, 250, 252);
    private static final float MARGIN = 50f;
    private static final float PAGE_W = 595.28f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;
    private static BaseFont baseFont;

    private static BaseFont getBaseFont() {
        if (baseFont == null) {
            try { baseFont = BaseFont.createFont(); } catch (Exception ignored) {}
        }
        return baseFont;
    }

    public byte[] generateFormat(String formatId) {
        try {
            return switch (formatId) {
                case "permisos" -> generatePermiso();
                case "vacaciones" -> generateVacaciones();
                case "compensacion-vacaciones" -> generateCompensacionVacaciones();
                case "retiro-cesantias" -> generateRetiroCesantias();
                case "reembolso-gastos" -> generateReembolsoGastos();
                case "dia-familia" -> generateDiaFamilia();
                case "horas-extras" -> generateHorasExtras();
                default -> null;
            };
        } catch (Exception e) {
            log.error("Error generando formato {}: {}", formatId, e.getMessage(), e);
            return null;
        }
    }

    private PdfWriter initDoc(ByteArrayOutputStream out) throws Exception {
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        doc.open();
        return writer;
    }

    private void addHeader(Document doc, String code, String title) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1f, 2.5f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph logoP = new Paragraph();
        logoP.add(new Chunk("SPM", new Font(Font.HELVETICA, 22, Font.BOLD, NAVY)));
        logoP.add(new Chunk("\n", new Font(Font.HELVETICA, 6)));
        logoP.add(new Chunk("MARDIQUE", new Font(Font.HELVETICA, 8, Font.BOLD, GOLD)));
        logoCell.addElement(logoP);
        header.addCell(logoCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph info = new Paragraph();
        info.setAlignment(Element.ALIGN_RIGHT);
        info.add(new Chunk("Formato: " + code, new Font(Font.HELVETICA, 8, Font.BOLD, NAVY)));
        info.add(new Chunk("\nSPM — Mardique", new Font(Font.HELVETICA, 7, Font.NORMAL, GRAY)));
        infoCell.addElement(info);
        header.addCell(infoCell);

        doc.add(header);
        doc.add(new Paragraph("\n"));

        PdfPTable titleBar = new PdfPTable(1);
        titleBar.setWidthPercentage(100);
        PdfPCell tc = new PdfPCell(new Paragraph(title, new Font(Font.HELVETICA, 14, Font.BOLD, NAVY)));
        tc.setBackgroundColor(new Color(240, 244, 248));
        tc.setBorderColor(LIGHT_GRAY);
        tc.setBorderWidth(1);
        tc.setPadding(10);
        tc.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleBar.addCell(tc);
        doc.add(titleBar);
        doc.add(new Paragraph("\n"));
    }

    private void addFooter(PdfWriter writer, int pageNum) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        cb.setColorStroke(LIGHT_GRAY);
        cb.moveTo(MARGIN, 35);
        cb.lineTo(PAGE_W - MARGIN, 35);
        cb.stroke();
        cb.beginText();
        cb.setFontAndSize(getBaseFont(), 7);
        cb.setColorFill(GRAY);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT, "SPM — Mardique", MARGIN, 24, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, "Página " + pageNum, PAGE_W / 2, 24, 0);
        cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, "Confidencial", PAGE_W - MARGIN, 24, 0);
        cb.endText();
        cb.restoreState();
    }

    private Paragraph labelParagraph(String text) {
        Font lf = new Font(Font.HELVETICA, 8, Font.BOLD, NAVY);
        Paragraph p = new Paragraph(text, lf);
        p.setSpacingAfter(2);
        return p;
    }

    private Paragraph fieldLine(String label) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ", new Font(Font.HELVETICA, 9, Font.BOLD, new Color(51, 65, 85))));
        p.add(new Chunk("_".repeat(50), new Font(Font.HELVETICA, 9, Font.NORMAL, LIGHT_GRAY)));
        p.setSpacingAfter(6);
        return p;
    }

    private Paragraph multilineField(String label, int lines) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ":", new Font(Font.HELVETICA, 9, Font.BOLD, new Color(51, 65, 85))));
        for (int i = 0; i < lines; i++) {
            p.add(new Chunk("\n" + "_".repeat(85), new Font(Font.HELVETICA, 9, Font.NORMAL, LIGHT_GRAY)));
        }
        p.setSpacingAfter(8);
        return p;
    }

    private PdfPTable twoColField(String l1, String l2) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});
        t.addCell(createLightCell(l1 + ": " + "_".repeat(28)));
        t.addCell(createLightCell(l2 + ": " + "_".repeat(28)));
        return t;
    }

    private PdfPTable threeColField(String l1, String l2, String l3) {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f, 1f});
        t.addCell(createLightCell(l1 + ": " + "_".repeat(16)));
        t.addCell(createLightCell(l2 + ": " + "_".repeat(16)));
        t.addCell(createLightCell(l3 + ": " + "_".repeat(16)));
        return t;
    }

    private PdfPCell createLightCell(String text) {
        PdfPCell c = new PdfPCell(new Paragraph(text, new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(4);
        return c;
    }

    private void addSignatureBlock(Document doc, String... labels) throws Exception {
        doc.add(new Paragraph("\n"));
        PdfPTable sigTable = new PdfPTable(labels.length);
        sigTable.setWidthPercentage(100);
        float[] widths = new float[labels.length];
        for (int i = 0; i < labels.length; i++) widths[i] = 1f;
        sigTable.setWidths(widths);

        for (String label : labels) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            cell.setFixedHeight(80);
            cell.addElement(new Paragraph("", new Font(Font.HELVETICA, 1)));
            cell.addElement(new Paragraph("_".repeat(30), new Font(Font.HELVETICA, 9, Font.NORMAL, LIGHT_GRAY)));
            cell.addElement(new Paragraph("\n" + label, new Font(Font.HELVETICA, 8, Font.BOLD, NAVY)));
            cell.addElement(new Paragraph("Firma y C.C.", new Font(Font.HELVETICA, 7, Font.ITALIC, GRAY)));
            sigTable.addCell(cell);
        }
        doc.add(sigTable);
    }

    private void addCheckboxTable(Document doc, String title, String[] items) throws Exception {
        if (title != null) {
            doc.add(labelParagraph(title));
        }
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});
        for (String item : items) {
            PdfPCell c = new PdfPCell(new Paragraph("☐  " + item, new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(51, 65, 85))));
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(3);
            t.addCell(c);
        }
        if (items.length % 2 != 0) {
            t.addCell(new PdfPCell(new Paragraph("", new Font(Font.HELVETICA, 1))));
        }
        doc.add(t);
        doc.add(new Paragraph("\n"));
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-008: SOLICITUD DE PERMISO
    // ═══════════════════════════════════════════════════════════════

    private byte[] generatePermiso() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-008", "FORMATO DE SOLICITUD DE PERMISO");

        doc.add(fieldLine("Nombre completo del trabajador"));
        doc.add(threeColField("Cédula", "Cargo", "Área / Departamento"));
        doc.add(fieldLine("Fecha de solicitud"));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, "TIPO DE PERMISO", new String[]{
                "Permiso personal remunerado", "Permiso médico (con soporte)",
                "Permiso académico/institucional", "Permiso por luto",
                "Permiso de maternidad/paternidad", "Otro (especificar)"
        });

        doc.add(fieldLine("Fecha(s) del permiso"));
        doc.add(fieldLine("Horario (entrada — salida)"));
        doc.add(multilineField("Motivo / Justificación", 3));
        doc.add(fieldLine("Nombre del jefe inmediato"));
        doc.add(fieldLine("Soportes adjuntos (si aplica)"));

        addSignatureBlock(doc, "Firma del Trabajador", "Firma Jefe Inmediato", "Talento Humano");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-009: SOLICITUD DE VACACIONES
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateVacaciones() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-009", "FORMATO DE SOLICITUD DE VACACIONES");

        doc.add(fieldLine("Nombre completo del trabajador"));
        doc.add(threeColField("Cédula", "Cargo", "Área / Departamento"));
        doc.add(fieldLine("Fecha de solicitud"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("PERÍODO A DISFRUTAR"));
        doc.add(twoColField("Fecha de inicio", "Fecha de finalización"));
        doc.add(fieldLine("Número de días naturales"));
        doc.add(fieldLine("Número de días hábiles"));

        doc.add(new Paragraph("\n"));
        doc.add(fieldLine("Dirección de destino"));
        doc.add(fieldLine("Teléfono de contacto"));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, null, new String[]{
                "Las vacaciones no exceden 15 días consecutivos",
                "Se solicita con al menos 15 días de anticipación",
                "Entregué el formato a mi jefe directo"
        });

        doc.add(fieldLine("Nombre del jefe inmediato"));

        addSignatureBlock(doc, "Firma del Trabajador", "Firma Jefe Inmediato", "Talento Humano");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-010: COMPENSACIÓN DE VACACIONES
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateCompensacionVacaciones() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-010", "FORMATO DE COMPENSACIÓN DE VACACIONES");

        doc.add(fieldLine("Nombre completo del trabajador"));
        doc.add(threeColField("Cédula", "Cargo", "Área / Departamento"));
        doc.add(fieldLine("Fecha de solicitud"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("DÍAS A COMPENSAR"));
        doc.add(twoColField("Fecha de ingreso a la empresa", "Días acumulados"));
        doc.add(twoColField("Días a compensar (números)", "Días restantes"));
        doc.add(fieldLine("Valor a compensar (si aplica)"));

        doc.add(new Paragraph("\n"));
        doc.add(multilineField("Observaciones / Motivo de la compensación", 3));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, null, new String[]{
                "Solicito compensación económica por vacaciones",
                "Solicito compensación en tiempo libre",
                "Adjunto soportes de los días a compensar"
        });

        addSignatureBlock(doc, "Firma del Trabajador", "Firma Gerencia", "Talento Humano");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-022: RETIRO PARCIAL DE CESANTÍAS (2 PÁGINAS)
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateRetiroCesantias() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-022", "FORMATO DE RETIRO PARCIAL DE CESANTÍAS");

        doc.add(fieldLine("Nombre completo del trabajador"));
        doc.add(twoColField("Cédula", "Cargo"));
        doc.add(fieldLine("Área / Departamento"));
        doc.add(fieldLine("Fecha de solicitud"));
        doc.add(fieldLine("Fecha de ingreso a la empresa"));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, "MOTIVO DEL RETIRO", new String[]{
                "Vivienda", "Estudio", "Enfermedad grave", "Pensionado por vejez",
                "Terminación del contrato", "Otro (especificar)"
        });

        doc.add(fieldLine("Fondo de cesantías (nombre)"));
        doc.add(fieldLine("Monto a retirar ($)"));
        doc.add(multilineField("Justificación / Descripción del motivo", 3));
        doc.add(fieldLine("Documentos adjuntos"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("DATOS DEL FONDO"));
        doc.add(fieldLine("Nombre del fondo"));
        doc.add(twoColField("NIT del fondo", "Número de cuenta"));
        doc.add(fieldLine("Dirección / Teléfono del fondo"));

        addSignatureBlock(doc, "Firma del Trabajador", "Firma Autorización RRHH");

        addFooter(writer, 1);

        // ── PÁGINA 2 ──
        doc.newPage();

        doc.add(new Paragraph("\n"));
        PdfPTable p2Title = new PdfPTable(1);
        p2Title.setWidthPercentage(100);
        PdfPCell tc2 = new PdfPCell(new Paragraph("RETIRO PARCIAL DE CESANTÍAS — CONTINUACIÓN",
                new Font(Font.HELVETICA, 12, Font.BOLD, NAVY)));
        tc2.setBackgroundColor(new Color(240, 244, 248));
        tc2.setBorderColor(LIGHT_GRAY);
        tc2.setBorderWidth(1);
        tc2.setPadding(8);
        tc2.setHorizontalAlignment(Element.ALIGN_CENTER);
        p2Title.addCell(tc2);
        doc.add(p2Title);
        doc.add(new Paragraph("\n"));

        doc.add(labelParagraph("DECLARACIÓN DEL TRABAJADOR"));
        doc.add(multilineField("Declaro que los recursos solicitados serán destinados exclusivamente para el fin declarado", 4));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, null, new String[]{
                "Adjunto carta del fondo de cesantías",
                "Adjunto comprobante de uso del recurso",
                "Autorizo el descuento de la nómina si aplica"
        });

        doc.add(fieldLine("Número de radicado del fondo"));
        doc.add(fieldLine("Fecha de respuesta esperada"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("OBSERVACIONES TALENTO HUMANO"));
        doc.add(multilineField("Observaciones", 3));

        addSignatureBlock(doc, "Firma del Trabajador", "Firma Autorización RRHH", "Firma Gerencia");

        addFooter(writer, 2);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-025: REEMBOLSO DE GASTOS
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateReembolsoGastos() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-025", "FORMATO DE REEMBOLSO DE GASTOS");

        doc.add(fieldLine("Nombre completo del colaborador"));
        doc.add(twoColField("Cédula", "Cargo"));
        doc.add(fieldLine("Área / Departamento"));
        doc.add(fieldLine("Fecha de solicitud"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("DETALLE DE GASTOS"));

        PdfPTable detailTable = new PdfPTable(4);
        detailTable.setWidthPercentage(100);
        detailTable.setWidths(new float[]{0.4f, 3f, 1.2f, 1.4f});

        Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        String[] headers = {"No.", "Descripción del gasto", "Valor ($)", "Fecha"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Paragraph(h, thFont));
            hc.setBackgroundColor(NAVY);
            hc.setPadding(5);
            detailTable.addCell(hc);
        }
        for (int i = 1; i <= 6; i++) {
            detailTable.addCell(createLightCell(String.valueOf(i)));
            detailTable.addCell(createLightCell("_".repeat(40)));
            detailTable.addCell(createLightCell("$" + "_".repeat(12)));
            detailTable.addCell(createLightCell("_".repeat(12)));
        }
        doc.add(detailTable);

        doc.add(new Paragraph("\n"));
        doc.add(fieldLine("Total a reembolsar"));
        doc.add(fieldLine("Forma de pago preferida"));

        doc.add(new Paragraph("\n"));
        doc.add(multilineField("Observaciones", 2));

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, null, new String[]{
                "Adjunto facturas/recibos originales",
                "Los gastos son de naturaleza operativa",
                "Aprobo el gasto previamente con mi jefe"
        });

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("FIRMAS"));
        Paragraph sigNote = new Paragraph("La firma de \"Quien Recibe\" corresponde al Coordinador, Jefe o Talento Humano seleccionado por el colaborador.",
                new Font(Font.HELVETICA, 7, Font.ITALIC, GRAY));
        sigNote.setSpacingAfter(6);
        doc.add(sigNote);
        addSignatureBlock(doc, "Firma del Colaborador", "Firma de Quien Recibe");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-026: CARTA SOLICITUD DÍA DE LA FAMILIA
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateDiaFamilia() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-026", "CARTA SOLICITUD DÍA DE LA FAMILIA");

        doc.add(new Paragraph("\n"));
        Paragraph saludo = new Paragraph("Ciudad y fecha: " + "_".repeat(40),
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85)));
        saludo.setSpacingAfter(12);
        doc.add(saludo);

        doc.add(new Paragraph("Señor(a)", new Font(Font.HELVETICA, 10, Font.BOLD, new Color(51, 65, 85))));
        doc.add(fieldLine("Nombre de quien autoriza"));
        doc.add(fieldLine("Cargo"));
        doc.add(fieldLine("Área / Departamento"));

        doc.add(new Paragraph("\n"));
        Paragraph body = new Paragraph();
        body.add(new Chunk("Estimado(a) ", new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85))));
        body.add(new Chunk("_".repeat(40), new Font(Font.HELVETICA, 10, Font.NORMAL, LIGHT_GRAY)));
        body.add(new Chunk("\n\nPor medio de la presente, solicito amablemente la autorización para hacer uso del Día de la Familia, ",
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85))));
        body.add(new Chunk("conforme a lo estipulado en la legislación laboral vigente y las políticas de la empresa.",
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85))));
        body.setSpacingAfter(12);
        doc.add(body);

        doc.add(fieldLine("Nombre del solicitante"));
        doc.add(twoColField("Cédula", "Cargo"));
        doc.add(fieldLine("Área / Departamento"));
        doc.add(fieldLine("Fecha del día de la familia"));
        doc.add(fieldLine("Nombre del grupo familiar"));

        doc.add(new Paragraph("\n"));
        Paragraph cierre = new Paragraph(
                "Sin otro particular, quedo a su disposición para cualquier consulta.\n\nAtentamente,",
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85)));
        cierre.setSpacingAfter(20);
        doc.add(cierre);

        Paragraph sigNote = new Paragraph("La firma de \"Quien Autoriza\" corresponde al Coordinador, Jefe o Talento Humano seleccionado por el colaborador.",
                new Font(Font.HELVETICA, 7, Font.ITALIC, GRAY));
        sigNote.setSpacingAfter(6);
        doc.add(sigNote);
        addSignatureBlock(doc, "Firma del Solicitante", "Firma de Quien Autoriza");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════
    // FT-GATH-SPM-004/027: HORAS EXTRAS Y RECARGOS
    // ═══════════════════════════════════════════════════════════════

    private byte[] generateHorasExtras() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = initDoc(out);
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, 40, 50);
        doc.open();

        addHeader(doc, "FT-GATH-SPM-004 / 027", "FORMATO DE HORAS EXTRAS Y RECARGOS");

        doc.add(fieldLine("Nombre completo del trabajador"));
        doc.add(threeColField("Cédula", "Cargo", "Área / Departamento"));
        doc.add(twoColField("Fecha de solicitud", "Período de pago"));

        doc.add(new Paragraph("\n"));
        doc.add(labelParagraph("HORAS EXTRA / RECARGOS REALIZADOS"));

        PdfPTable hoursTable = new PdfPTable(5);
        hoursTable.setWidthPercentage(100);
        hoursTable.setWidths(new float[]{1.2f, 1.2f, 1.5f, 1f, 1f});

        Font thFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        String[] headers = {"Fecha", "Horario", "Tipo de recargo", "Horas", "Valor ($)"};
        for (String h : headers) {
            PdfPCell hc = new PdfPCell(new Paragraph(h, thFont));
            hc.setBackgroundColor(NAVY);
            hc.setPadding(5);
            hoursTable.addCell(hc);
        }
        for (int i = 1; i <= 8; i++) {
            hoursTable.addCell(createLightCell("_".repeat(12)));
            hoursTable.addCell(createLightCell("_".repeat(16)));
            hoursTable.addCell(createLightCell("_".repeat(20)));
            hoursTable.addCell(createLightCell("_".repeat(8)));
            hoursTable.addCell(createLightCell("$" + "_".repeat(10)));
        }
        doc.add(hoursTable);

        doc.add(new Paragraph("\n"));
        addCheckboxTable(doc, "TIPO DE RECARGO", new String[]{
                "Hora extra diurna (125%)", "Hora extra nocturna (175%)",
                "Recargo dominical (175%)", "Recargo festivo (175%)",
                "Hora extra diurna dominical (200%)"
        });

        doc.add(fieldLine("Total horas extras"));
        doc.add(fieldLine("Total recargos"));
        doc.add(fieldLine("Total a pagar"));
        doc.add(multilineField("Observaciones", 2));

        addSignatureBlock(doc, "Firma del Colaborador", "Firma Jefe Inmediato");

        addFooter(writer, 1);
        doc.close();
        return out.toByteArray();
    }
}
