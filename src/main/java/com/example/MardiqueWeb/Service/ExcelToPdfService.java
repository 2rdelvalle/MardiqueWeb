package com.example.MardiqueWeb.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ExcelToPdfService {

    private static final Logger log = LoggerFactory.getLogger(ExcelToPdfService.class);

    private static final String LO_BIN = resolveLibreOfficeBin();
    private static final int TIMEOUT_SECONDS = 60;

    public byte[] convertExcelToPdf(byte[] excelBytes, String fileName) throws IOException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return doConvertExcelToPdf(excelBytes, fileName);
            } catch (IOException e) {
                lastError = e;
                log.warn("Intento {}/3 de LibreOffice falló: {}", attempt, e.getMessage());
                if (attempt < 3) {
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        throw lastError != null ? lastError : new IOException("LibreOffice falló después de 3 intentos");
    }

    private byte[] doConvertExcelToPdf(byte[] excelBytes, String fileName) throws IOException {
        Path tempDir = Files.createTempDirectory("lo_conv_" + UUID.randomUUID());

        try {
            String ext = getExtension(fileName);
            Path inputFile = tempDir.resolve("documento" + ext);
            Files.write(inputFile, excelBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    LO_BIN,
                    "--headless",
                    "--norestore",
                    "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toString(),
                    inputFile.toAbsolutePath().toString()
            );
            pb.environment().put("HOME", tempDir.toString());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice tardó más de " + TIMEOUT_SECONDS + "s");
            }

            int exitCode = process.exitValue();
            log.debug("LibreOffice output: {}", output);

            if (exitCode != 0) {
                throw new IOException("LibreOffice terminó con código " + exitCode + ": " + output);
            }

            String baseName = inputFile.getFileName().toString().replaceAll("\\.[^.]+$", "");
            Path pdfPath = tempDir.resolve(baseName + ".pdf");

            if (!Files.exists(pdfPath)) {
                pdfPath = Files.list(tempDir)
                        .filter(p -> p.toString().endsWith(".pdf"))
                        .findFirst()
                        .orElseThrow(() -> new IOException("LibreOffice no generó ningún PDF. Output: " + output));
            }

            // Truncar el PDF resultante para conservar ÚNICAMENTE la página 1 y descartar las demás
            try {
                byte[] rawPdfBytes = Files.readAllBytes(pdfPath);
                com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(rawPdfBytes);
                if (reader.getNumberOfPages() > 1) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    com.lowagie.text.Document document = new com.lowagie.text.Document(reader.getPageSize(1));
                    com.lowagie.text.pdf.PdfCopy copy = new com.lowagie.text.pdf.PdfCopy(document, bos);
                    document.open();
                    copy.addPage(copy.getImportedPage(reader, 1));
                    document.close();
                    Files.write(pdfPath, bos.toByteArray());
                }
                reader.close();
            } catch (Exception e) {
                log.warn("No se pudo truncar el PDF a 1 página, se mantiene el documento completo: {}", e.getMessage());
            }

            return Files.readAllBytes(pdfPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversión interrumpida", e);
        } finally {
            deleteDir(tempDir);
        }
    }

    private static String resolveLibreOfficeBin() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] candidates = {
                    "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                    "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
            };
            for (String path : candidates) {
                if (new File(path).exists()) return path;
            }
            return "soffice.exe";
        } else {
            String[] candidates = {
                    "/usr/bin/libreoffice",
                    "/usr/bin/soffice",
                    "/opt/libreoffice/program/soffice",
                    "/Applications/LibreOffice.app/Contents/MacOS/soffice"
            };
            for (String path : candidates) {
                if (new File(path).exists()) return path;
            }
            return "libreoffice";
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) return ".xlsx";
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : ".xlsx";
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("No se pudo limpiar directorio temporal: {}", dir, e);
        }
    }
}