package com.example.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TableAnalyzerService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QueryAIService queryAIService;

    public Map<String, Object> analyzeTable(String tableName, int sampleSize, int maxDistinctValues) throws Exception {
        try {
            // Fetch table sample data
            String sampleQuery = String.format("SELECT * FROM \"%s\" ORDER BY RANDOM() LIMIT %d", tableName, sampleSize);
            List<Map<String, Object>> rows = jdbcTemplate.query(sampleQuery, this::mapRow);

            if (rows.isEmpty()) {
                throw new RuntimeException("No data found in the table.");
            }

            Set<String> columns = rows.get(0).keySet();
            Map<String, Object> dataDictionary = new HashMap<>();

            for (String column : columns) {
                List<Object> columnData = rows.stream()
                        .map(row -> row.get(column))
                        .collect(Collectors.toList());

                Set<Object> distinctValues = new HashSet<>(columnData);
                long nullCount = columnData.stream().filter(Objects::isNull).count();

                String columnType = getColumnType(tableName, column);

                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("type", getHumanReadableType(columnType));
                columnInfo.put("distinctCount", distinctValues.size());
                columnInfo.put("nullCount", nullCount);
                columnInfo.put("nullPercentage", (double) nullCount / sampleSize * 100);

                // Top values for small distinct sets
                if (distinctValues.size() <= maxDistinctValues) {
                    Map<Object, Long> valueCounts = columnData.stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

                    List<Map<String, Object>> topValues = valueCounts.entrySet().stream()
                            .sorted(Map.Entry.<Object, Long>comparingByValue().reversed())
                            .limit(5)
                            .map(entry -> Map.of("value", entry.getKey(), "count", entry.getValue()))
                            .collect(Collectors.toList());

                    columnInfo.put("topValues", topValues);
                }

                // Numeric columns
                if ("number".equals(getHumanReadableType(columnType))) {
                    List<Double> numericValues = columnData.stream()
                            .filter(Objects::nonNull)
                            .map(value -> Double.parseDouble(value.toString()))
                            .collect(Collectors.toList());
                    if (!numericValues.isEmpty()) {
                        columnInfo.put("min", Collections.min(numericValues));
                        columnInfo.put("max", Collections.max(numericValues));
                    }
                }

                // Date columns
                if ("date".equals(getHumanReadableType(columnType))) {
                    List<Date> dateValues = columnData.stream()
                            .filter(Objects::nonNull)
                            .map(value -> (Date) value)
                            .collect(Collectors.toList());
                    if (!dateValues.isEmpty()) {
                        columnInfo.put("min", Collections.min(dateValues));
                        columnInfo.put("max", Collections.max(dateValues));
                    }
                }

                dataDictionary.put(column, columnInfo);
            }

            // Generate summary using AI
            return generateLLMSummary(dataDictionary);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze table: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> generateLLMSummary(Map<String, Object> dataDictionary) throws Exception {
        String systemPrompt = "You are a data analyst tasked with providing concise but complete descriptions of data fields. Include data type and notable information about sample values where relevant. Be brief but ensure every field is described.";
        String userPrompt = String.format("Given the following data dictionary, provide a description for each field:\n\n%s\n\nFormat your response as a JSON object where keys are field names and values are descriptions.",
                new ObjectMapper().writeValueAsString(dataDictionary));

        String aiResponse = queryAIService.queryAI(systemPrompt, userPrompt);
        return new ObjectMapper().readValue(aiResponse, Map.class);
    }

    private String getColumnType(String tableName, String columnName) {
        String query = String.format("SELECT data_type FROM information_schema.columns WHERE table_name = '%s' AND column_name = '%s'", tableName, columnName);
        return jdbcTemplate.queryForObject(query, String.class);
    }

    private String getHumanReadableType(String type) {
        switch (type) {
            case "boolean":
                return "boolean";
            case "bigint":
            case "smallint":
            case "integer":
            case "real":
            case "double precision":
            case "numeric":
                return "number";
            case "date":
            case "timestamp":
            case "timestamp with time zone":
                return "date";
            case "character varying":
            case "text":
                return "text";
            default:
                return "unknown";
        }
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
        }
        return row;
    }
}
