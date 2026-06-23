package com.events.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Gerencia a conexão com MongoDB.
 * Banco: events_db
 * Coleções: events, participants
 */
public class MongoDBConfig {

    private static final String URI      = "mongodb://localhost:27017";
    private static final String DB_NAME  = "events_db";

    private static MongoClient   client;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            client   = MongoClients.create(URI);
            database = client.getDatabase(DB_NAME);
        }
        return database;
    }

    public static void close() {
        if (client != null) {
            client.close();
        }
    }
}
