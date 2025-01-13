package com.example.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Timestamp;

@Component
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseService(
            @Value("${db.user}") String user,
            @Value("${db.password}") String password,
            @Value("${db.host}") String host,
            @Value("${db.port}") int port,
            @Value("${db.name}") String databaseName
    ) {
        this.jdbcTemplate = new JdbcTemplate(createDataSource(user, password, host, port, databaseName));
    }

    private DataSource createDataSource(String user, String password, String host, int port, String databaseName) {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    @PostConstruct
    public void initializeTables() {
        String createTableQuery = """
            CREATE TABLE IF NOT EXISTS TABLE_SCHEMA (
                table_name TEXT PRIMARY KEY,
                analysis JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        jdbcTemplate.execute(createTableQuery);
    }

    public int executeUpdate(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

    public <T> T executeQuery(String sql, Object[] params, Class<T> returnType) {
        return jdbcTemplate.queryForObject(sql, params, returnType);
    }
}
