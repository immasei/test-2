package com.example.test.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.postgresql.util.PGobject;
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

    String DEFAULT_TABLE_SCHEMA = "public.table_schema";

    @Autowired
    OpenAiService openAiService;

    public void initializeTable() {
        // initialize table to store llm analysis about table structure
        // TODO combine db analysis and stored prompt -> get smr analysis from llm
        // also data type & table structure from describe table
        String sql = String.format(
        "CREATE TABLE IF NOT EXISTS %s (" +
        "    schema_name TEXT, " +
        "    table_name TEXT, " +
        "    analysis JSONB, " +
        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
        "    PRIMARY KEY(schema_name, table_name))", DEFAULT_TABLE_SCHEMA);
        jdbcTemplate.execute(sql);
    }

    public List<Map<String, Object>> listTables() {
        // [ {schemaname=?, table_name=?}, {}, {} ]
        String sql = """
        SELECT schemaname, tablename
        FROM pg_tables t
        WHERE t.tableowner = current_user
            AND schemaname NOT IN ('pg_catalog', 'information_schema')
            AND tablename != 'table_schema';
        """;

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

    public String getNote() {
        return "N/A\n";
    }

    public void generateLLMSummary(String schemaName, String tableName) {
        String systemPrompt = "You're a data analyst tasked with providing VERY concise but complete descriptions of data table. Include data type and any notable information in one description as value of field. Be brief but ensure every field, constraints, relationship is described.";
        String userInput = "Given the following table structure and notes from owner, provide a description for each field. Format your response as a JSON object where key are fields names and value are descriptions. The list of keys should be 'columns', 'index', 'foreign key', 'referenced by' and 'trigger'. The information can be found under table structure, if it's not found, make it null.\n" +
                describeTable(schemaName, tableName) +
                "\n==============SPECIAL NOTE====================\n" +
                getNote();

//        System.out.println(userInput);
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>)
                    openAiService.generateResponse(systemPrompt, userInput).get("choices");

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
            String llmAnalysis = (String) messageMap.get("content");

            System.out.println(llmAnalysis);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonbAnalysis = mapper.readTree(llmAnalysis);
            insertAnalysis(schemaName, tableName, jsonbAnalysis);

        } catch (Exception e) {
            throw new RuntimeException("Error :D: " + e.getMessage(), e);
        }
//        return CompletableFuture.runAsync(() -> {
//            try {
//                List<Map<String, Object>> choices = (List<Map<String, Object>>)
//                        openAiService.generateResponse(systemPrompt, userInput).get("choices");
//
//                Map<String, Object> firstChoice = choices.get(0);
//                Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
//                String llmAnalysis = (String) messageMap.get("content");
//
//                ObjectMapper mapper = new ObjectMapper();
//                JsonNode jsonbAnalysis = mapper.readTree(llmAnalysis);
//                insertAnalysis(schemaName, tableName, jsonbAnalysis);
//
//            } catch (Exception e) {
//                throw new RuntimeException("Error :D: " + e.getMessage(), e);
//            }
//        });
    }

    public void runLLMAnalysis() {
        List<Map<String, Object>> tables = listTables();
        System.out.println(tables);

        for (Map<String, Object> table : tables) {
            generateLLMSummary(table.get("schemaname").toString(), table.get("tablename").toString());
        }
//
//        // thread pool
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//
//        // submit tasks
//        List<CompletableFuture<Void>> futures = tables.stream()
//                .map(table -> {
//                    String schemaName = (String) table.get("schemaname");
//                    String tableName = (String) table.get("tablename");
//                    return CompletableFuture.runAsync(() ->
//                          generateLLMSummary(schemaName, tableName).join(), executor);
//                })
//                .toList();
//
//        // wait
//        CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//        allTasks.thenRun(() -> System.out.println("All tasks completed successfully."))
//                .exceptionally(e -> {
//                    System.err.println("Error in parallel tasks: " + e.getMessage());
//                    return null;
//                })
//                .join(); // Block until all tasks are finished
//
//
//        executor.shutdown();
    }

    public void insertAnalysis(String schemaName, String tableName, JsonNode analysis) {
        String sql = String.format(
        "INSERT INTO %s (schema_name, table_name, analysis)\n" +
        "VALUES (?, ?, ?)\n" +
        "ON CONFLICT (schema_name, table_name)\n" +
        "DO UPDATE SET\n" +
        "   analysis=EXCLUDED.analysis,\n" +
        "   updated_at=CURRENT_TIMESTAMP", DEFAULT_TABLE_SCHEMA);

        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(analysis.toString()); // Convert JsonNode to String

            jdbcTemplate.update(sql, schemaName, tableName, jsonObject);
        } catch (Exception e) {
            throw new RuntimeException("Error inserting analysis", e);
        }
    }





}
