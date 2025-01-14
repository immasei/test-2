package com.example.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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



}
