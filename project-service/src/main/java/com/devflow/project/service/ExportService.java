package com.devflow.project.service;

import com.devflow.project.entity.Task;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

@Service
public class ExportService {

    // ── QR Code Generation ────────────────────────────────────

    public String generateTaskQrCodeDataUri(Task task, String baseUrl) {
        try {
            String url = baseUrl + "/tasks/" + task.getKey();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 250, 250);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            String base64Image = Base64.getEncoder().encodeToString(pngData);
            return "data:image/png;base64," + base64Image;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    // ── PDF Document Generation ───────────────────────────────

    public byte[] exportTasksToPdf(String projectName, List<Task> tasks) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("DevFlow Task Export - " + projectName).setFontSize(18).setBold());
            document.add(new Paragraph("Generated at: " + java.time.Instant.now().toString()).setFontSize(10));
            document.add(new Paragraph("\n"));

            float[] columnWidths = {80F, 200F, 100F, 100F};
            Table table = new Table(columnWidths);

            table.addHeaderCell(new Cell().add(new Paragraph("Key").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Title").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Priority").setBold()));

            for (Task task : tasks) {
                table.addCell(new Cell().add(new Paragraph(task.getKey())));
                table.addCell(new Cell().add(new Paragraph(task.getTitle())));
                table.addCell(new Cell().add(new Paragraph(task.getStatus().name())));
                table.addCell(new Cell().add(new Paragraph(task.getPriority().name())));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    // ── Excel Document Generation ─────────────────────────────

    public byte[] exportTasksToExcel(List<Task> tasks) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Tasks");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Key", "Title", "Status", "Priority", "Assignee ID", "Reporter ID", "Story Points"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data rows
            int rowIdx = 1;
            for (Task task : tasks) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(task.getKey());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getStatus().name());
                row.createCell(3).setCellValue(task.getPriority().name());
                row.createCell(4).setCellValue(task.getAssigneeId() != null ? task.getAssigneeId().toString() : "");
                row.createCell(5).setCellValue(task.getReporterId().toString());
                row.createCell(6).setCellValue(task.getStoryPoints() != null ? task.getStoryPoints() : 0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }
}
