package com.example.test.controller;

import com.example.test.service.TableAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Text2SqlController {
    @Autowired
    TableAnalyzerService tableAnalyzerService;

    @GetMapping("/tables")
    public ResponseEntity<String> getTables() {
        System.out.println(tableAnalyzerService.listTables("opaltravel"));
        System.out.println(tableAnalyzerService.analyzeTable("opaltravel.opalcards", 100, 100));
        return ResponseEntity.ok("tables");
    }
}
