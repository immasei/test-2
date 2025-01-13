package com.example.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryProcessorService {

    @Autowired
    private QueryAIService queryAIService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> processQuery(String message) {
        Map<String, Object> finalResponse = new HashMap<>();
        try {
            System.out.println("🚀 Processing query: " + message);

            // Step 1: Triage the request
            Map<String, String> triagePrompt = getTriagePrompt(message);
            String triageResponse = queryAIService.queryAI(triagePrompt.get("system"), triagePrompt.get("user"));
            System.out.println("📋 Step 1 - Triage AI response: " + triageResponse);

            Map<String, Object> triageResult = objectMapper.readValue(triageResponse, Map.class);
            String queryType = (String) triageResult.get("queryType");
            String response;

            switch (queryType) {
                case "GENERAL_QUESTION":
                    response = processGeneralQuestion(message);
                    break;

                case "DATA_QUESTION":
                    response = processDataQuestion(message);
                    break;

                case "OUT_OF_SCOPE":
                default:
                    response = "I apologize, but this question appears to be outside the scope of database-related queries I can help with.";
                    break;
            }

            finalResponse.put("response", response);
            finalResponse.put("timestamp", new java.util.Date().toString());
            finalResponse.put("queryType", queryType);

        } catch (Exception e) {
            e.printStackTrace();
            finalResponse.put("error", "Failed to process query: " + e.getMessage());
        }
        return finalResponse;
    }

    private Map<String, String> getTriagePrompt(String message) {
        String systemPrompt = "Triage system prompt here.";
        String userPrompt = "Triage user prompt: " + message;
        return Map.of("system", systemPrompt, "user", userPrompt);
    }

    private String processGeneralQuestion(String message) throws Exception {
        String systemPrompt = "General question system prompt.";
        String userPrompt = "General question user prompt: " + message;

        String aiResponse = queryAIService.queryAI(systemPrompt, userPrompt);
        Map<String, Object> generalResult = objectMapper.readValue(aiResponse, Map.class);
        return (String) generalResult.get("answer");
    }

    private String processDataQuestion(String message) throws Exception {
        // Step 3: Get schema information
        List<Map<String, Object>> schemaRows = jdbcTemplate.queryForList("SELECT table_name, analysis FROM TABLE_SCHEMA");
        System.out.println("📊 Step 3 - Schema query result: " + schemaRows);

        Map<String, Object> schemaData = Map.of("tables", schemaRows);
        String schemaPrompt = getSchemaAnalysisPrompt(schemaData, message);

        // Step 4: Analyze schema
        String schemaResponse = queryAIService.queryAI("Schema analysis system prompt.", schemaPrompt);
        Map<String, Object> schemaAnalysis = objectMapper.readValue(schemaResponse, Map.class);

        boolean inScope = (boolean) schemaAnalysis.getOrDefault("inScope", false);
        if (!inScope) {
            return "I apologize, but I cannot answer this question using the available database schema. " +
                    schemaAnalysis.getOrDefault("outOfScopeReason", "No reason provided.");
        }

        // Step 5: SQL generation and query execution
        return processSQLGenerationAndExecution(schemaAnalysis, message);
    }

    private String processSQLGenerationAndExecution(Map<String, Object> schemaAnalysis, String message) throws Exception {
        int attempts = 0;
        final int MAX_ATTEMPTS = 3;
        String sqlQuery = "";
        String lastError = "";

        while (attempts < MAX_ATTEMPTS) {
            attempts++;
            try {
                String sqlPrompt = lastError.isEmpty()
                        ? getGenerateSQLPrompt(schemaAnalysis, message)
                        : getRegenerateSQLPrompt(schemaAnalysis, message, sqlQuery, lastError);

                String sqlResponse = queryAIService.queryAI("SQL generation system prompt.", sqlPrompt);
                Map<String, String> sqlResult = objectMapper.readValue(sqlResponse, Map.class);
                sqlQuery = sqlResult.get("query");

                List<Map<String, Object>> queryResults = jdbcTemplate.queryForList(sqlQuery);
                System.out.println("⚡ Step 6 - Query results: " + queryResults);

                // Format the response
                String formatPrompt = getFormatAnswerPrompt(message, sqlQuery, queryResults);
                String formattedResponse = queryAIService.queryAI("Format answer system prompt.", formatPrompt);

                Map<String, Object> formattedResult = objectMapper.readValue(formattedResponse, Map.class);

                // Validate the response
                String validatePrompt = getValidateAnswerPrompt(message, (String) formattedResult.get("answer"));
                String validationResponse = queryAIService.queryAI("Validate answer system prompt.", validatePrompt);
                Map<String, Object> validationResult = objectMapper.readValue(validationResponse, Map.class);

                boolean isValid = (boolean) validationResult.getOrDefault("isAnswered", false);
                if (isValid) {
                    return (String) formattedResult.get("answer");
                } else {
                    lastError = (String) validationResult.getOrDefault("reason", "Answer does not address the question");
                }
            } catch (Exception e) {
                lastError = e.getMessage();
                System.err.println("❌ Error in attempt " + attempts + ": " + e.getMessage());
            }
        }
        return "I apologize, but I was unable to generate a satisfactory answer to your question after multiple attempts. " +
                "The last error encountered was: " + lastError;
    }

    private String getSchemaAnalysisPrompt(Map<String, Object> schemaData, String message) {
        // Return schema analysis prompt as a String.
        return "Schema analysis user prompt with schemaData and message.";
    }

    private String getGenerateSQLPrompt(Map<String, Object> schemaAnalysis, String message) {
        // Return SQL generation prompt as a String.
        return "Generate SQL user prompt with schemaAnalysis and message.";
    }

    private String getRegenerateSQLPrompt(Map<String, Object> schemaAnalysis, String message, String lastQuery, String error) {
        // Return regenerate SQL prompt as a String.
        return "Regenerate SQL user prompt with schemaAnalysis, message, lastQuery, and error.";
    }

    private String getFormatAnswerPrompt(String message, String sqlQuery, List<Map<String, Object>> queryResults) {
        // Return format answer prompt as a String.
        return "Format answer user prompt with message, sqlQuery, and queryResults.";
    }

    private String getValidateAnswerPrompt(String message, String answer) {
        // Return validate answer prompt as a String.
        return "Validate answer user prompt with message and answer.";
    }
}
