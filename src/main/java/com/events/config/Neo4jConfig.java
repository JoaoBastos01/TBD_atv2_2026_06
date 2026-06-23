package com.events.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * Gerencia a conexão com Neo4j.
 * URI padrão: bolt://localhost:7687
 * Nós  : Pessoa, Evento
 * Rels : ORGANIZOU, PARTICIPOU
 */
public class Neo4jConfig {

    private static final String URI      = "bolt://localhost:7688";
    private static final String USER     = "neo4j";
    private static final String PASSWORD = "senha123";

    private static Driver driver;

    public static Driver getDriver() {
        if (driver == null) {
            driver = GraphDatabase.driver(URI, AuthTokens.basic(USER, PASSWORD));
        }
        return driver;
    }

    public static void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
