package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.service.PdfExportService;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class PdfExportController {
    private final PdfExportService pdfExportService;
    private final SecurityUtils securityUtils;

    @GetMapping(value = "/reading-list", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportReadingList() {
        Long userId = securityUtils.getCurrentUserId();
        byte[] pdfBytes = pdfExportService.exportReadingList(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reading-list.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}