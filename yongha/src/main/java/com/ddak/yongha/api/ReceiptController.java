package com.ddak.yongha.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ddak.yongha.service.ReceiptOcrService;
import com.ddak.yongha.vo.ReceiptData;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receipt")
public class ReceiptController {

    private final ReceiptOcrService ocrService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptData> parse(@RequestPart("file") MultipartFile file) throws Exception {
    	ReceiptData data = ocrService.ocr(file.getBytes());
        
        return ResponseEntity.ok(data);
    }
}
