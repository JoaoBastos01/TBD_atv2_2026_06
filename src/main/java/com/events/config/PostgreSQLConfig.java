package com.events.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexão com PostgreSQL.
 * Banco: events_sql
 * Tabelas: eventos, participantes, participacoes
 */
public class PostgreSQLConfig {

    private static final String URL      = "jdbc:postgresql://localhost:5433/events_sql";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "1234";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }

    /** Cria o schema inicial caso não exista. */
    public static void initSchema() throws SQLException {
        String sqlEventos = """
            CREATE TABLE IF NOT EXISTS eventos (
                id          VARCHAR(100) PRIMARY KEY,
                nome        VARCHAR(255) NOT NULL,
                data        VARCHAR(50),
                local       VARCHAR(255),
                descricao   TEXT,
                palavras_chave TEXT
            );
            """;

        String sqlParticipantes = """
            CREATE TABLE IF NOT EXISTS participantes (
                id    VARCHAR(100) PRIMARY KEY,
                nome  VARCHAR(255) NOT NULL,
                email VARCHAR(255)
            );
            """;

        String sqlParticipacoes = """
            CREATE TABLE IF NOT EXISTS participacoes (
                participante_id VARCHAR(100) REFERENCES participantes(id),
                evento_id       VARCHAR(100) REFERENCES eventos(id),
                papel           VARCHAR(50) CHECK (papel IN ('ORGANIZADOR','PARTICIPANTE')),
                PRIMARY KEY (participante_id, evento_id, papel)
            );
            """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sqlEventos);
            stmt.execute(sqlParticipantes);
            stmt.execute(sqlParticipacoes);
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
