package com.events.export;

import com.events.model.Evento;
import com.events.model.Participacao;
import com.events.model.Participante;
import com.events.repository.PostgreSQLRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

/**
 * Exportação para base de dados SQL (Requisito 6).
 *
 * Dois modos:
 *  1. exportarParaPostgres()  → sincroniza dados do MongoDB → PostgreSQL
 *  2. gerarArquivoSQL()       → gera script .sql com os INSERTs
 */
public class SqlExporter {

    private final PostgreSQLRepository sqlRepo;

    public SqlExporter() {
        this.sqlRepo = new PostgreSQLRepository();
    }

    /**
     * Sincroniza eventos e participantes do MongoDB para o PostgreSQL.
     * Usa upsert (INSERT ... ON CONFLICT DO UPDATE).
     */
    public void exportarParaPostgres(List<Evento> eventos, List<Participante> participantes)
            throws SQLException {

        System.out.println("[SQL] Exportando " + eventos.size() + " eventos para PostgreSQL...");
        for (Evento e : eventos) {
            sqlRepo.upsertEvento(e);
        }

        System.out.println("[SQL] Exportando " + participantes.size() + " participantes para PostgreSQL...");
        for (Participante p : participantes) {
            sqlRepo.upsertParticipante(p);
        }

        System.out.println("[SQL] Exportação para PostgreSQL concluída.");
    }

    /**
     * Gera um arquivo .sql com scripts de INSERT para todos os dados.
     * Útil para migração ou backup.
     *
     * @param eventos       lista de eventos
     * @param participantes lista de participantes
     * @param caminho       caminho do arquivo de saída (ex: "exports/dump.sql")
     */
    public static void gerarArquivoSQL(List<Evento> eventos, List<Participante> participantes, String caminho)
            throws IOException {

        new java.io.File(caminho).getParentFile().mkdirs();
        List<Participacao> participacoes = listarParticipacoesParaDump();

        try (PrintWriter pw = new PrintWriter(new FileWriter(caminho))) {

            pw.println("-- ============================================");
            pw.println("-- Exportação SQL - Sistema de Gerenciamento de Eventos");
            pw.println("-- Gerado em: " + java.time.LocalDateTime.now());
            pw.println("-- ============================================");
            pw.println();

            // Schema
            pw.println("-- SCHEMA");
            pw.println("""
CREATE TABLE IF NOT EXISTS eventos (
    id            VARCHAR(100) PRIMARY KEY,
    nome          VARCHAR(255) NOT NULL,
    data          VARCHAR(50),
    local         VARCHAR(255),
    descricao     TEXT,
    palavras_chave TEXT
);

CREATE TABLE IF NOT EXISTS participantes (
    id    VARCHAR(100) PRIMARY KEY,
    nome  VARCHAR(255) NOT NULL,
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS participacoes (
    participante_id VARCHAR(100) REFERENCES participantes(id),
    evento_id       VARCHAR(100) REFERENCES eventos(id),
    papel           VARCHAR(50) CHECK (papel IN ('ORGANIZADOR','PARTICIPANTE')),
    PRIMARY KEY (participante_id, evento_id, papel)
);
""");

            // Eventos
            pw.println("-- EVENTOS");
            for (Evento e : eventos) {
                pw.printf(
                    "INSERT INTO eventos (id, nome, data, local, descricao, palavras_chave) " +
                    "VALUES ('%s', '%s', '%s', '%s', '%s', '%s') " +
                    "ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, data=EXCLUDED.data, " +
                    "local=EXCLUDED.local, descricao=EXCLUDED.descricao, palavras_chave=EXCLUDED.palavras_chave;%n",
                    esc(e.getId()), esc(e.getNome()), esc(e.getData()),
                    esc(e.getLocal()), esc(e.getDescricao()),
                    esc(String.join(",", e.getPalavrasChave()))
                );
            }
            pw.println();

            // Participantes
            pw.println("-- PARTICIPANTES");
            for (Participante p : participantes) {
                pw.printf(
                    "INSERT INTO participantes (id, nome, email) " +
                    "VALUES ('%s', '%s', '%s') " +
                    "ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, email=EXCLUDED.email;%n",
                    esc(p.getId()), esc(p.getNome()), esc(p.getEmail())
                );
            }

            pw.println();
            pw.println("-- PARTICIPACOES");
            for (Participacao part : participacoes) {
                pw.printf(
                    "INSERT INTO participacoes (participante_id, evento_id, papel) " +
                    "VALUES ('%s', '%s', '%s') ON CONFLICT DO NOTHING;%n",
                    esc(part.getParticipanteId()), esc(part.getEventoId()), esc(part.getPapel().name())
                );
            }

            pw.println();
            pw.println("-- FIM DO DUMP");
        }

        System.out.println("[SQL] Arquivo SQL gerado em: " + caminho);
    }

    /** Escapa aspas simples para SQL. */
    private static String esc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private static List<Participacao> listarParticipacoesParaDump() throws IOException {
        try {
            return new PostgreSQLRepository().listarParticipacoes();
        } catch (SQLException ex) {
            throw new IOException("Erro ao listar participações para o dump SQL: " + ex.getMessage(), ex);
        }
    }
}
