package com.events.repository;

import com.events.config.PostgreSQLConfig;
import com.events.model.Evento;
import com.events.model.Papel;
import com.events.model.Participacao;
import com.events.model.Participante;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório PostgreSQL.
 * Usado principalmente para exportação SQL (Requisito 6)
 * e como fonte de dados no Dashboard (Requisito 1).
 */
public class PostgreSQLRepository {

    // ==================== EVENTOS ====================

    public void upsertEvento(Evento e) throws SQLException {
        String sql = """
            INSERT INTO eventos (id, nome, data, local, descricao, palavras_chave)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
              SET nome=EXCLUDED.nome, data=EXCLUDED.data,
                  local=EXCLUDED.local, descricao=EXCLUDED.descricao,
                  palavras_chave=EXCLUDED.palavras_chave
            """;
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getNome());
            ps.setString(3, e.getData());
            ps.setString(4, e.getLocal());
            ps.setString(5, e.getDescricao());
            ps.setString(6, String.join(",", e.getPalavrasChave()));
            ps.executeUpdate();
        }
    }

    public List<Evento> listarEventos() throws SQLException {
        List<Evento> lista = new ArrayList<>();
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM eventos ORDER BY data")) {
            while (rs.next()) {
                lista.add(mapEvento(rs));
            }
        }
        return lista;
    }

    public Evento buscarEventoPorId(String id) throws SQLException {
        String sql = "SELECT * FROM eventos WHERE id=?";
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapEvento(rs) : null;
            }
        }
    }

    public List<Evento> filtrarEventos(String local, String data, String palavraChave) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM eventos WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (local != null && !local.isBlank()) {
            sql.append(" AND local ILIKE ?");
            params.add("%" + local.trim() + "%");
        }
        if (data != null && !data.isBlank()) {
            sql.append(" AND data = ?");
            params.add(data.trim());
        }
        if (palavraChave != null && !palavraChave.isBlank()) {
            sql.append(" AND (nome ILIKE ? OR descricao ILIKE ? OR palavras_chave ILIKE ?)");
            String termo = "%" + palavraChave.trim() + "%";
            params.add(termo);
            params.add(termo);
            params.add(termo);
        }

        sql.append(" ORDER BY data");

        List<Evento> lista = new ArrayList<>();
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapEvento(rs));
                }
            }
        }
        return lista;
    }

    public void deletarEvento(String id) throws SQLException {
        try (PreparedStatement ps = PostgreSQLConfig.getConnection()
                .prepareStatement("DELETE FROM participacoes WHERE evento_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = PostgreSQLConfig.getConnection()
                .prepareStatement("DELETE FROM eventos WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // ==================== PARTICIPANTES ====================

    public void upsertParticipante(Participante p) throws SQLException {
        String sql = """
            INSERT INTO participantes (id, nome, email)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE
              SET nome=EXCLUDED.nome, email=EXCLUDED.email
            """;
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getEmail());
            ps.executeUpdate();
        }
    }

    public List<Participante> listarParticipantes() throws SQLException {
        List<Participante> lista = new ArrayList<>();
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM participantes ORDER BY nome")) {
            while (rs.next()) {
                lista.add(new Participante(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("email")
                ));
            }
        }
        return lista;
    }

    public Participante buscarParticipantePorId(String id) throws SQLException {
        String sql = "SELECT * FROM participantes WHERE id=?";
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new Participante(rs.getString("id"), rs.getString("nome"), rs.getString("email"))
                        : null;
            }
        }
    }

    public void deletarParticipante(String id) throws SQLException {
        try (PreparedStatement ps = PostgreSQLConfig.getConnection()
                .prepareStatement("DELETE FROM participacoes WHERE participante_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = PostgreSQLConfig.getConnection()
                .prepareStatement("DELETE FROM participantes WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    // ==================== PARTICIPAÇÕES ====================

    public void upsertParticipacao(String participanteId, String eventoId, Papel papel) throws SQLException {
        String sql = """
            INSERT INTO participacoes (participante_id, evento_id, papel)
            VALUES (?, ?, ?)
            ON CONFLICT DO NOTHING
            """;
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, participanteId);
            ps.setString(2, eventoId);
            ps.setString(3, papel.name());
            ps.executeUpdate();
        }
    }

    public void deletarParticipacao(String participanteId, String eventoId, Papel papel) throws SQLException {
        String sql = "DELETE FROM participacoes WHERE participante_id=? AND evento_id=? AND papel=?";
        try (PreparedStatement ps = PostgreSQLConfig.getConnection().prepareStatement(sql)) {
            ps.setString(1, participanteId);
            ps.setString(2, eventoId);
            ps.setString(3, papel.name());
            ps.executeUpdate();
        }
    }

    public List<Participacao> listarParticipacoes() throws SQLException {
        List<Participacao> lista = new ArrayList<>();
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT participante_id, evento_id, papel FROM participacoes ORDER BY evento_id, participante_id, papel")) {
            while (rs.next()) {
                lista.add(new Participacao(
                        rs.getString("participante_id"),
                        rs.getString("evento_id"),
                        Papel.valueOf(rs.getString("papel"))
                ));
            }
        }
        return lista;
    }

    // ==================== ESTATÍSTICAS PARA DASHBOARD ====================

    public int contarEventos() throws SQLException {
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM eventos")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarParticipantes() throws SQLException {
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM participantes")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarOrganizadores() throws SQLException {
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(DISTINCT participante_id) FROM participacoes WHERE papel='ORGANIZADOR'")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int contarParticipacoes() throws SQLException {
        try (Statement st = PostgreSQLConfig.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM participacoes")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Evento mapEvento(ResultSet rs) throws SQLException {
        Evento ev = new Evento(
                rs.getString("id"),
                rs.getString("nome"),
                rs.getString("data"),
                rs.getString("local"),
                rs.getString("descricao")
        );

        String palavras = rs.getString("palavras_chave");
        if (palavras != null && !palavras.isBlank()) {
            for (String palavra : palavras.split(",")) {
                if (!palavra.isBlank()) {
                    ev.getPalavrasChave().add(palavra.trim());
                }
            }
        }
        return ev;
    }
}
