package com.example.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TableAnalyzerService {
    // TODO sep by
    //  lv0: server
    //  lv1: db
    //  lv2: schema

    String username = "postgres";
    String database = "postgres";
    String password = "343567";

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void initializeTable() {
        // initialize table to store llm analysis about table structure
        // TODO combine db analysis and stored prompt -> get smr analysis from llm
        // also data type & table structure from describe table
        String sql = """
        CREATE TABLE IF NOT EXISTS table_schema (
            table_name TEXT PRIMARY KEY,
            analysis JSONB,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """;
    }

    public List<Map<String, Object>> listTables(String tableSchema) {
        // [{table_name=customers}]
        // TODO schema as var: Schema names in PostgreSQL are case-sensitive.
        // variation in some versions: BASE TABLE | BASE_TABLE
        // SELECT DISTINCT table_type FROM information_schema.tables;
        String sql = String.format(
        "SELECT table_name " +
        "FROM information_schema.tables " +
        "WHERE table_schema='%s' AND table_type LIKE '%%BASE%%'", tableSchema);

        return jdbcTemplate.queryForList(sql);
    }

    public String describeTable(String schemaName, String tableName) {
        // describe table structure with psql: \d <tablename>
        List<String> commands = new ArrayList<>();
        commands.add("psql");
        commands.add("-U");
        commands.add(username);
        commands.add("-d");
        commands.add(database);
        commands.add("-c");
        commands.add("\\d " + schemaName + "." + tableName);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.environment().put("PGPASSWORD", password);

            Process process = processBuilder.start();

            // capture output
            BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder stdout = new StringBuilder();

            String line;
            while ((line = stdin.readLine()) != null) {
                stdout.append(line).append("\n");
            }

            process.waitFor();
            return stdout.toString().trim();
        } catch (Exception e) {
            return "error w psql \\d: " + e.getMessage();
        }
    }

    public Map<String, Object> analyzeTable(String tableName, int sampleSize, int maxDistinctValues) {
        Map<String, Object> dataDictionary = new HashMap<>();

        try {
            // Fetch sample data
            String sampleQuery = String.format(
                    "SELECT * FROM %s " +
                    "ORDER BY RANDOM() LIMIT %d", tableName, sampleSize
            );
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sampleQuery);

            if (rows.isEmpty()) {
                throw new RuntimeException("Table is empty or invalid table name provided.");
            }

            // Get column metadata
            Set<String> columns = rows.get(0).keySet();

            for (String column : columns) {
                schemaName = "opaltravel";
                String columnType = getColumnType(schemaName, tableName, column);
                List<Object> columnData = rows.stream()
                        .map(row -> row.get(column))
                        .collect(Collectors.toList());
                Map<String, Object> columnAnalysis = analyzeColumn(column, columnData, columnType, sampleSize, maxDistinctValues);
                dataDictionary.put(column, columnAnalysis);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error analyzing table: " + e.getMessage(), e);
        }

        return dataDictionary;
    }

    private Map<String, Object> analyzeColumn(String columnName, List<Object> columnData, String columnType, int sampleSize, int maxDistinctValues) {
        switch (columnType.toLowerCase()) {
            case "number":
                return analyzeNumericColumn(columnData, sampleSize);
            case "text":
            case "varchar":
            case "char":
                return analyzeTextColumn(columnData, maxDistinctValues);
            case "json":
                return analyzeJsonColumn(columnData);
            case "date":
            case "timestamp":
                return analyzeDateColumn(columnData);
            case "array":
                return analyzeArrayColumn(columnData);
            default:
                return analyzeGenericColumn(columnData, sampleSize, maxDistinctValues);
        }
    }

    private Map<String, Object> analyzeNumericColumn(List<Object> columnData, int sampleSize) {
        Map<String, Object> columnInfo = new HashMap<>();
        List<Number> numericValues = columnData.stream()
                .filter(value -> value instanceof Number)
                .map(value -> (Number) value)
                .collect(Collectors.toList());

        if (!numericValues.isEmpty()) {
            List<Double> values = numericValues.stream().map(Number::doubleValue).collect(Collectors.toList());
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double median = values.size() % 2 == 0
                    ? (values.get(values.size() / 2 - 1) + values.get(values.size() / 2)) / 2.0
                    : values.get(values.size() / 2);
            double variance = values.stream()
                    .mapToDouble(value -> Math.pow(value - mean, 2))
                    .average()
                    .orElse(0);
            double stddev = Math.sqrt(variance);

            columnInfo.put("min", Collections.min(values));
            columnInfo.put("max", Collections.max(values));
            columnInfo.put("mean", mean);
            columnInfo.put("median", median);
            columnInfo.put("stddev", stddev);
        }
        return columnInfo;
    }

    private Map<String, Object> analyzeTextColumn(List<Object> columnData, int maxDistinctValues) {
        Map<String, Object> columnInfo = new HashMap<>();
        Set<Object> distinctValues = new HashSet<>(columnData);
        columnInfo.put("distinctCount", distinctValues.size());

        if (distinctValues.size() <= maxDistinctValues) {
            Map<Object, Long> valueCounts = columnData.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

            List<Map<String, Object>> topValues = valueCounts.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .map(entry -> Map.of("value", entry.getKey(), "count", entry.getValue()))
                    .collect(Collectors.toList());

            columnInfo.put("topValues", topValues);
        }
        return columnInfo;
    }

    private Map<String, Object> analyzeDateColumn(List<Object> columnData) {
        Map<String, Object> columnInfo = new HashMap<>();
        List<Date> dateValues = columnData.stream()
                .filter(value -> value instanceof Date)
                .map(value -> (Date) value)
                .collect(Collectors.toList());

        if (!dateValues.isEmpty()) {
            columnInfo.put("min", Collections.min(dateValues));
            columnInfo.put("max", Collections.max(dateValues));
        }
        return columnInfo;
    }

    private Map<String, Object> analyzeJsonColumn(List<Object> columnData) {
        Map<String, Object> columnInfo = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        List<JsonNode> jsonNodes = columnData.stream()
                .filter(Objects::nonNull)
                .map(value -> {
                    try {
                        return objectMapper.readTree(value.toString());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        columnInfo.put("jsonAnalysis", Map.of(
                "averageKeys", jsonNodes.stream()
                        .mapToInt(JsonNode::size)
                        .average()
                        .orElse(0)
        ));
        return columnInfo;
    }

    private Map<String, Object> analyzeArrayColumn(List<Object> columnData) {
        Map<String, Object> columnInfo = new HashMap<>();
        List<List<Object>> arrays = columnData.stream()
                .filter(value -> value instanceof List)
                .map(value -> (List<Object>) value)
                .collect(Collectors.toList());

        if (!arrays.isEmpty()) {
            columnInfo.put("averageArraySize", arrays.stream()
                    .mapToInt(List::size)
                    .average()
                    .orElse(0));
        }
        return columnInfo;
    }

    private Map<String, Object> analyzeGenericColumn(List<Object> columnData, int sampleSize, int maxDistinctValues) {
        Map<String, Object> columnInfo = new HashMap<>();
        long nullCount = columnData.stream().filter(Objects::isNull).count();
        Set<Object> distinctValues = new HashSet<>(columnData);

        columnInfo.put("nullCount", nullCount);
        columnInfo.put("nullPercentage", (nullCount / (double) sampleSize) * 100);
        columnInfo.put("distinctCount", distinctValues.size());

        return columnInfo;
    }

    private Map<String, String> getColumnTypes(String schemaName, String tableName) {
        String query = "SELECT column_name, data_type " +
                "FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ?";

        return jdbcTemplate.query(query, new Object[]{schemaName, tableName}, rs -> {
            Map<String, String> columnTypes = new HashMap<>();
            while (rs.next()) {
                columnTypes.put(rs.getString("column_name"), rs.getString("data_type"));
            }
            return columnTypes;
        });
    }


//    private String getColumnType(String schemaName, String tableName, String columnName) {
//        String query = "SELECT data_type " +
//                "FROM information_schema.columns " +
//                "WHERE table_schema = ? AND table_name = ? AND column_name = ?";
//
//        return jdbcTemplate.queryForObject(query, new Object[]{schemaName, tableName, columnName}, String.class);
//    }










//    private Map<String, Object> analyzeColumn(
//            String columnName, List<Object> columnData, int sampleSize, int maxDistinctValues
//    ) {
//        Map<String, Object> columnInfo = new HashMap<>();
//
//        // Null count and percentage
//        long nullCount = columnData.stream().filter(Objects::isNull).count();
//        columnInfo.put("nullCount", nullCount);
//        columnInfo.put("nullPercentage", (nullCount / (double) sampleSize) * 100);
//
//        // Distinct values
//        Set<Object> distinctValues = new HashSet<>(columnData);
//        columnInfo.put("distinctCount", distinctValues.size());
//
//        if (distinctValues.size() <= maxDistinctValues) {
//            // Count occurrences of each value
//            Map<Object, Long> valueCounts = columnData.stream()
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
//
//            // Top values (most frequent)
//            List<Map<String, Object>> topValues = valueCounts.entrySet().stream()
//                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
//                    .limit(5)
//                    .map(entry -> Map.of("value", entry.getKey(), "count", entry.getValue()))
//                    .collect(Collectors.toList());
//            columnInfo.put("topValues", topValues);
//        }
//
//        // Min and Max for numeric or date columns
//        if (columnData.stream().allMatch(value -> value instanceof Number)) {
//            List<Number> numericValues = columnData.stream()
//                    .filter(value -> value != null && value instanceof Number)
//                    .map(value -> (Number) value)
//                    .collect(Collectors.toList());
//
//            if (!numericValues.isEmpty()) {
//                columnInfo.put("min", numericValues.stream().mapToDouble(Number::doubleValue).min().orElse(0));
//                columnInfo.put("max", numericValues.stream().mapToDouble(Number::doubleValue).max().orElse(0));
//            }
//        } else if (columnData.stream().allMatch(value -> value instanceof Date)) {
//            List<Date> dateValues = columnData.stream()
//                    .filter(value -> value != null && value instanceof Date)
//                    .map(value -> (Date) value)
//                    .collect(Collectors.toList());
//
//            if (!dateValues.isEmpty()) {
//                columnInfo.put("min", Collections.min(dateValues));
//                columnInfo.put("max", Collections.max(dateValues));
//            }
//        }
//
//        return columnInfo;
//    }
}
