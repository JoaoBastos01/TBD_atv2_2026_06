package com.events.repository;

import com.events.config.MongoDBConfig;
import com.events.model.Evento;
import com.events.model.Participante;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositório MongoDB para Eventos e Participantes.
 *
 * Funcionalidades:
 *  - CRUD completo de Eventos e Participantes
 *  - Filtros dinâmicos: local, data, palavra-chave (requisito 4)
 */
public class MongoRepository {

    private static final String COL_EVENTOS      = "events";
    private static final String COL_PARTICIPANTES = "participants";

    private final MongoDatabase db;

    public MongoRepository() {
        this.db = MongoDBConfig.getDatabase();
    }

    // ==================== COLEÇÕES ====================

    private MongoCollection<Document> eventos() {
        return db.getCollection(COL_EVENTOS);
    }

    private MongoCollection<Document> participantes() {
        return db.getCollection(COL_PARTICIPANTES);
    }

    // ==================== EVENTO - CRUD ====================

    public void criarEvento(Evento e) {
        Document doc = new Document("_id", e.getId())
                .append("nome", e.getNome())
                .append("data", e.getData())
                .append("local", e.getLocal())
                .append("descricao", e.getDescricao())
                .append("palavrasChave", e.getPalavrasChave());
        eventos().insertOne(doc);
    }

    public Evento buscarEventoPorId(String id) {
        Document doc = eventos().find(Filters.eq("_id", id)).first();
        return doc != null ? docToEvento(doc) : null;
    }

    public List<Evento> listarTodosEventos() {
        List<Evento> lista = new ArrayList<>();
        for (Document doc : eventos().find()) {
            lista.add(docToEvento(doc));
        }
        return lista;
    }

    public long contarEventos() {
        return eventos().countDocuments();
    }

    public void atualizarEvento(Evento e) {
        Document update = new Document("$set",
                new Document("nome", e.getNome())
                        .append("data", e.getData())
                        .append("local", e.getLocal())
                        .append("descricao", e.getDescricao())
                        .append("palavrasChave", e.getPalavrasChave()));
        eventos().updateOne(Filters.eq("_id", e.getId()), update);
    }

    public void deletarEvento(String id) {
        eventos().deleteOne(Filters.eq("_id", id));
    }

    // ==================== FILTROS DINÂMICOS (Requisito 4) ====================

    /**
     * Filtra eventos dinamicamente. Qualquer parâmetro pode ser null (ignorado).
     *
     * @param local       filtra por local (case-insensitive, parcial)
     * @param data        filtra por data exata ("YYYY-MM-DD")
     * @param palavraChave busca dentro de nome, descrição ou palavrasChave
     */
    public List<Evento> filtrarEventos(String local, String data, String palavraChave) {
        List<Bson> filtros = new ArrayList<>();

        if (local != null && !local.isBlank()) {
            filtros.add(Filters.regex("local", local, "i"));
        }
        if (data != null && !data.isBlank()) {
            filtros.add(Filters.eq("data", data));
        }
        if (palavraChave != null && !palavraChave.isBlank()) {
            filtros.add(Filters.or(
                    Filters.regex("nome", palavraChave, "i"),
                    Filters.regex("descricao", palavraChave, "i"),
                    Filters.regex("palavrasChave", palavraChave, "i")
            ));
        }

        List<Evento> resultado = new ArrayList<>();
        Bson filtroFinal = filtros.isEmpty() ? new Document() : Filters.and(filtros);

        for (Document doc : eventos().find(filtroFinal)) {
            resultado.add(docToEvento(doc));
        }
        return resultado;
    }

    // ==================== PARTICIPANTE - CRUD ====================

    public void criarParticipante(Participante p) {
        Document doc = new Document("_id", p.getId())
                .append("nome", p.getNome())
                .append("email", p.getEmail());
        participantes().insertOne(doc);
    }

    public Participante buscarParticipantePorId(String id) {
        Document doc = participantes().find(Filters.eq("_id", id)).first();
        return doc != null ? docToParticipante(doc) : null;
    }

    public List<Participante> listarTodosParticipantes() {
        List<Participante> lista = new ArrayList<>();
        for (Document doc : participantes().find()) {
            lista.add(docToParticipante(doc));
        }
        return lista;
    }

    public long contarParticipantes() {
        return participantes().countDocuments();
    }

    public void atualizarParticipante(Participante p) {
        Document update = new Document("$set",
                new Document("nome", p.getNome())
                        .append("email", p.getEmail()));
        participantes().updateOne(Filters.eq("_id", p.getId()), update);
    }

    public void deletarParticipante(String id) {
        participantes().deleteOne(Filters.eq("_id", id));
    }

    // ==================== CONVERSORES PRIVADOS ====================

    @SuppressWarnings("unchecked")
    private Evento docToEvento(Document doc) {
        Evento e = new Evento();
        e.setId(doc.getString("_id"));
        e.setNome(doc.getString("nome"));
        e.setData(doc.getString("data"));
        e.setLocal(doc.getString("local"));
        e.setDescricao(doc.getString("descricao"));
        List<String> kw = (List<String>) doc.get("palavrasChave");
        if (kw != null) e.setPalavrasChave(kw);
        return e;
    }

    private Participante docToParticipante(Document doc) {
        Participante p = new Participante();
        p.setId(doc.getString("_id"));
        p.setNome(doc.getString("nome"));
        p.setEmail(doc.getString("email"));
        return p;
    }
}
