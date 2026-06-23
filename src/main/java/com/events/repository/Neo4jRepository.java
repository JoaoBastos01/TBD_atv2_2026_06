package com.events.repository;

import com.events.config.Neo4jConfig;
import com.events.model.Papel;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repositório Neo4j para gerenciamento de relacionamentos.
 *
 * Nós  : (:Pessoa {id, nome}), (:Evento {id, nome})
 * Rels : (:Pessoa)-[:PARTICIPOU]->(:Evento)
 *        (:Pessoa)-[:ORGANIZOU]->(:Evento)
 *
 * Funcionalidades:
 *  - Criar nós de Pessoa e Evento
 *  - Criar relacionamento PARTICIPOU ou ORGANIZOU
 *  - Consultar ouvintes e organizadores de um evento (Requisito 2)
 *  - Migrar participante → organizador (Requisito 3)
 */
public class Neo4jRepository {

    private final Driver driver;

    public Neo4jRepository() {
        this.driver = Neo4jConfig.getDriver();
    }

    // ==================== NÓS ====================

    public void criarOuMergeNoPessoa(String id, String nome) {
        try (Session s = driver.session()) {
            s.run("""
                MERGE (p:Pessoa {id: $id})
                ON CREATE SET p.nome = $nome
                ON MATCH  SET p.nome = $nome
                """, Values.parameters("id", id, "nome", nome));
        }
    }

    public void criarOuMergeNoEvento(String id, String nome) {
        try (Session s = driver.session()) {
            s.run("""
                MERGE (e:Evento {id: $id})
                ON CREATE SET e.nome = $nome
                ON MATCH  SET e.nome = $nome
                """, Values.parameters("id", id, "nome", nome));
        }
    }

    public void deletarNoPessoa(String id) {
        try (Session s = driver.session()) {
            s.run("MATCH (p:Pessoa {id: $id}) DETACH DELETE p",
                    Values.parameters("id", id));
        }
    }

    public void deletarNoEvento(String id) {
        try (Session s = driver.session()) {
            s.run("MATCH (e:Evento {id: $id}) DETACH DELETE e",
                    Values.parameters("id", id));
        }
    }

    // ==================== RELACIONAMENTOS ====================

    /**
     * Cria um relacionamento entre pessoa e evento.
     * Usa MERGE para evitar duplicatas.
     * @param papel PARTICIPANTE → cria [:PARTICIPOU]; ORGANIZADOR → cria [:ORGANIZOU]
     */
    public void adicionarRelacionamento(String pessoaId, String eventoId, Papel papel) {
        String rel = papel == Papel.ORGANIZADOR ? "ORGANIZOU" : "PARTICIPOU";
        String query = String.format("""
            MATCH (p:Pessoa {id: $pessoaId}), (e:Evento {id: $eventoId})
            MERGE (p)-[:%s]->(e)
            """, rel);
        try (Session s = driver.session()) {
            s.run(query, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));
        }
    }

    /**
     * Remove um relacionamento específico.
     */
    public void removerRelacionamento(String pessoaId, String eventoId, Papel papel) {
        String rel = papel == Papel.ORGANIZADOR ? "ORGANIZOU" : "PARTICIPOU";
        String query = String.format("""
            MATCH (p:Pessoa {id: $pessoaId})-[r:%s]->(e:Evento {id: $eventoId})
            DELETE r
            """, rel);
        try (Session s = driver.session()) {
            s.run(query, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));
        }
    }

    // ==================== CONSULTAS (Requisito 2) ====================

    /**
     * Retorna lista de nomes de participantes (ouvintes) de um evento.
     * Apenas quem tem [:PARTICIPOU] sem [:ORGANIZOU].
     */
    public List<String> consultarSomenteParticipantes(String eventoId) {
        try (Session s = driver.session()) {
            Result r = s.run("""
                MATCH (p:Pessoa)-[:PARTICIPOU]->(e:Evento {id: $eventoId})
                WHERE NOT (p)-[:ORGANIZOU]->(e)
                RETURN p.nome AS nome, p.id AS id
                ORDER BY p.nome
                """, Values.parameters("eventoId", eventoId));
            return extractNomes(r);
        }
    }

    /**
     * Retorna lista de nomes de organizadores de um evento.
     * Apenas quem tem [:ORGANIZOU].
     */
    public List<String> consultarOrganizadores(String eventoId) {
        try (Session s = driver.session()) {
            Result r = s.run("""
                MATCH (p:Pessoa)-[:ORGANIZOU]->(e:Evento {id: $eventoId})
                RETURN p.nome AS nome, p.id AS id
                ORDER BY p.nome
                """, Values.parameters("eventoId", eventoId));
            return extractNomes(r);
        }
    }

    /**
     * Retorna lista de pessoas que participam E organizam o mesmo evento.
     */
    public List<String> consultarOrganizadoresETambemParticipantes(String eventoId) {
        try (Session s = driver.session()) {
            Result r = s.run("""
                MATCH (p:Pessoa)-[:ORGANIZOU]->(e:Evento {id: $eventoId})
                WHERE (p)-[:PARTICIPOU]->(e)
                RETURN p.nome AS nome, p.id AS id
                ORDER BY p.nome
                """, Values.parameters("eventoId", eventoId));
            return extractNomes(r);
        }
    }

    /**
     * Retorna todos os participantes de um evento (qualquer papel).
     */
    public List<Map<String, String>> consultarTodosComPapel(String eventoId) {
        List<Map<String, String>> resultado = new ArrayList<>();
        try (Session s = driver.session()) {
            Result r = s.run("""
                MATCH (p:Pessoa)-[rel]->(e:Evento {id: $eventoId})
                RETURN p.nome AS nome, p.id AS id, type(rel) AS papel
                ORDER BY p.nome
                """, Values.parameters("eventoId", eventoId));
            while (r.hasNext()) {
                var rec = r.next();
                resultado.add(Map.of(
                        "id",    rec.get("id").asString(),
                        "nome",  rec.get("nome").asString(),
                        "papel", rec.get("papel").asString()
                ));
            }
        }
        return resultado;
    }

    public long contarOrganizadores() {
        try (Session s = driver.session()) {
            Result r = s.run("""
                MATCH (p:Pessoa)-[:ORGANIZOU]->(:Evento)
                RETURN count(DISTINCT p) AS total
                """);
            return r.single().get("total").asLong();
        }
    }

    // ==================== MIGRAÇÃO (Requisito 3) ====================

    /**
     * Migra um participante (PARTICIPOU) para organizador (ORGANIZOU) em um evento.
     * - Cria [:ORGANIZOU] se não existir
     * - Remove [:PARTICIPOU] (a pessoa deixa de ser apenas ouvinte)
     *
     * ATENÇÃO: se quiser manter os dois papéis simultaneamente,
     * use adicionarRelacionamento(id, eventoId, ORGANIZADOR) sem remover PARTICIPOU.
     */
    public boolean migrarParticipanteParaOrganizador(String pessoaId, String eventoId) {
        // Verifica se a relação PARTICIPOU existe
        try (Session s = driver.session()) {
            Result check = s.run("""
                MATCH (p:Pessoa {id: $pessoaId})-[:PARTICIPOU]->(e:Evento {id: $eventoId})
                RETURN count(*) AS c
                """, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));
            long count = check.single().get("c").asLong();
            if (count == 0) return false;

            // Cria ORGANIZOU
            s.run("""
                MATCH (p:Pessoa {id: $pessoaId}), (e:Evento {id: $eventoId})
                MERGE (p)-[:ORGANIZOU]->(e)
                """, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));

            // Remove PARTICIPOU
            s.run("""
                MATCH (p:Pessoa {id: $pessoaId})-[r:PARTICIPOU]->(e:Evento {id: $eventoId})
                DELETE r
                """, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));
        }
        return true;
    }

    /**
     * Adiciona papel ORGANIZADOR sem remover PARTICIPOU,
     * permitindo que a mesma pessoa seja organizador e participante simultaneamente.
     */
    public void adicionarPapelOrganizador(String pessoaId, String eventoId) {
        try (Session s = driver.session()) {
            s.run("""
                MATCH (p:Pessoa {id: $pessoaId}), (e:Evento {id: $eventoId})
                MERGE (p)-[:ORGANIZOU]->(e)
                """, Values.parameters("pessoaId", pessoaId, "eventoId", eventoId));
        }
    }

    // ==================== AUXILIARES ====================

    private List<String> extractNomes(Result r) {
        List<String> nomes = new ArrayList<>();
        while (r.hasNext()) {
            var rec = r.next();
            nomes.add(rec.get("nome").asString() + " (id: " + rec.get("id").asString() + ")");
        }
        return nomes;
    }
}
