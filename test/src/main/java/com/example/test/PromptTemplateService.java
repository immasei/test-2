package com.example.test;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromptTemplateService {

    public Map<String, String> triage(String message) {
        String system = "Triage system prompt here.";
        String user = "Triage user prompt: " + message;
        return Map.of("system", system, "user", user);
    }

    public String generalAnswer(String message) {
        return "General answer prompt for: " + message;
    }

    public String schemaAnalysis(Map<String, Object> tables, String message) {
        return "Schema analysis prompt using tables and message.";
    }

    public String generateSQL(Map<String, Object> schemaAnalysis, String message) {
        return "SQL generation prompt using schemaAnalysis and message.";
    }

    public String regenerateSQL(Map<String, Object> schemaAnalysis, String message, String lastQuery, String error) {
        return "Regenerate SQL prompt with schemaAnalysis, message, lastQuery, and error.";
    }

    public String formatAnswer(String message, String sqlQuery, List<Map<String, Object>> rows) {
        return "Format answer prompt with message, sqlQuery, and rows.";
    }

    public String validateAnswer(String message, String answer) {
        return "Validate answer prompt with message and answer.";
    }
}
