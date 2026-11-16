package com.fzu.addressbook.controller;

import com.fzu.addressbook.dto.ImportResult;
import com.fzu.addressbook.service.ContactService;
import com.fzu.addressbook.service.ExcelService;
import java.net.URLEncoder;
import java.time.LocalDate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST endpoints for Excel export, template download and import.
 */
@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelService excelService;
    private final ContactService contactService;

    public ExcelController(ExcelService excelService, ContactService contactService) {
        this.excelService = excelService;
        this.contactService = contactService;
    }

    /**
     * Exports every contact to an Excel file.
     *
     * @return the workbook as a download
     */
    @GetMapping("/export")
    public ResponseEntity<Resource> export() {
        byte[] bytes = excelService.export(contactService.list(null, null, null));
        String filename = "通讯录_" + LocalDate.now() + ".xlsx";
        return download(bytes, filename);
    }

    /**
     * Downloads an empty template with example rows.
     *
     * @return the template as a download
     */
    @GetMapping("/template")
    public ResponseEntity<Resource> template() {
        byte[] bytes = excelService.template();
        return download(bytes, "通讯录导入模板.xlsx");
    }

    /**
     * Imports contacts from an uploaded workbook.
     *
     * @param file multipart file part named {@code file}
     * @return imported and failed counters
     */
    @PostMapping("/import")
    public ImportResult importFile(@RequestParam("file") MultipartFile file) {
        return excelService.importFrom(file);
    }

    private ResponseEntity<Resource> download(byte[] bytes, String filename) {
        String encoded;
        try {
            encoded = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException exception) {
            encoded = "addressbook.xlsx";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XLSX);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes));
    }
}
