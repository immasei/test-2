package com.example.test.controller;

import com.example.test.service.OpenAiService;
import com.example.test.service.TableAnalyzerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class Text2SqlController {
    @Autowired
    TableAnalyzerService tableAnalyzerService;

    @Autowired
    OpenAiService openAiService;

    @GetMapping("/tables")
    public ResponseEntity<String> getTables() {
        tableAnalyzerService.initializeTable();
        tableAnalyzerService.runLLMAnalysis();
//        System.out.println(tableAnalyzerService.describeTable("opaltravel", "cardtypes"));
//        System.out.println(tableAnalyzerService.analyzeTable("opaltravel.opalcards", 100, 100));

//        String systemPrompt = "You're a data analyst tasked with providing VERY concise but complete descriptions of data table. Include data type and any notable information in one description as value of field. Be brief but ensure every field, constraints, relationship is described.";
//        String userInput = "Given the following table structure and notes from owner, provide a description for each field. Format your response as a JSON object where key are fields names and value are descriptions. The list of keys should be 'columns', 'index', 'foreign key', 'referenced by' and 'trigger'. The information can be found under table structure, if it's not found, make it null.\n" +
//                tableAnalyzerService.describeTable("opaltravel", "opalcards") +
//                "\n==============SPECIAL NOTE====================\n" +
//                tableAnalyzerService.getNote();
//
//        System.out.println(userInput);
//
//        // openai response
//        List<Map<String, Object>> choices = (List<Map<String, Object>>) openAiService.generateResponse(systemPrompt, userInput).get("choices");
//        Map<String, Object> firstChoice = choices.get(0);
//        Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
//        String analysis = (String) messageMap.get("content");
//
//        ObjectMapper mapper= new ObjectMapper();
//
//        try {
//            System.out.println(analysis);
//            JsonNode nameNode = mapper.readTree(analysis);
//            tableAnalyzerService.insertAnalysis("public", "customers", nameNode);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        tableAnalyzerService.runLLMAnalysis();
        return ResponseEntity.ok("tables");
    }
}
