package com.example.test;

import com.example.service.QueryProcessorService;
import com.example.service.TableAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class IndexController {

    @Autowired
    private TableAnalyzerService tableAnalyzerService;

    @Autowired
    private QueryProcessorService queryProcessorService;

    @PostMapping("/upload-csv")
    public ResponseEntity<Map<String, Object>> uploadCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableName") String tableName) {
        try {
            Map<String, Object> response = tableAnalyzerService.uploadAndAnalyzeCSV(file, tableName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> processQuery(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            Map<String, Object> response = queryProcessorService.processQuery(message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
