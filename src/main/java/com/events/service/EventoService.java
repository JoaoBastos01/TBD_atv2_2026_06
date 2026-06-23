package com.events.service;

import com.events.model.Evento;
import com.events.model.Papel;
import com.events.model.Participante;
import com.events.repository.MongoRepository;
import com.events.repository.Neo4jRepository;
import com.events.repository.PostgreSQLRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Serviço de negócio que orquestra as três bases de dados:
 *  - MongoDB  → dados de eventos e participantes
 *  - Neo4j    → relacionamentos PARTICIPOU / ORGANIZOU
 *  - PostgreSQL → espelho SQL para exportação e dashboard
 */
public class EventoService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final MongoRepository     mongoRepo;
    private final Neo4jRepository     neo4jRepo;
    private final PostgreSQLRepository sqlRepo;

    public EventoService() {
        this.mongoRepo  = new MongoRepository();
        this.neo4jRepo  = new Neo4jRepository();
        this.sqlRepo    = new PostgreSQLRepository();
    }

    // ==================== CRIAR ====================

    /** Cria evento nas três bases. */
    public void criarEvento(Evento e) throws SQLException {
        validarEvento(e);
        mongoRepo.criarEvento(e);
        sqlRepo.upsertEvento(e);
        neo4jRepo.criarOuMergeNoEvento(e.getId(), e.getNome());
    }

    /** Cria participante nas três bases. */
    public void criarParticipante(Participante p) throws SQLException {
        validarParticipante(p);
        mongoRepo.criarParticipante(p);
        sqlRepo.upsertParticipante(p);
        neo4jRepo.criarOuMergeNoPessoa(p.getId(), p.getNome());
    }

    // ==================== LER ====================

    public Evento buscarEvento(String id) {
        return mongoRepo.buscarEventoPorId(id);
    }

    public List<Evento> listarEventos() {
        return mongoRepo.listarTodosEventos();
    }

    public Participante buscarParticipante(String id) {
        return mongoRepo.buscarParticipantePorId(id);
    }

    public List<Participante> listarParticipantes() {
        return mongoRepo.listarTodosParticipantes();
    }

    // ==================== ATUALIZAR ====================

    public void atualizarEvento(Evento e) throws SQLException {
        validarEvento(e);
        mongoRepo.atualizarEvento(e);
        sqlRepo.upsertEvento(e);
        neo4jRepo.criarOuMergeNoEvento(e.getId(), e.getNome());
    }

    public void atualizarParticipante(Participante p) throws SQLException {
        validarParticipante(p);
        mongoRepo.atualizarParticipante(p);
        sqlRepo.upsertParticipante(p);
        neo4jRepo.criarOuMergeNoPessoa(p.getId(), p.getNome());
    }

    // ==================== DELETAR ====================

    public void deletarEvento(String id) throws SQLException {
        mongoRepo.deletarEvento(id);
        sqlRepo.deletarEvento(id);
        neo4jRepo.deletarNoEvento(id);
    }

    public void deletarParticipante(String id) throws SQLException {
        mongoRepo.deletarParticipante(id);
        sqlRepo.deletarParticipante(id);
        neo4jRepo.deletarNoPessoa(id);
    }

    // ==================== RELACIONAMENTOS ====================

    /**
     * Adiciona um papel a uma pessoa em um evento (PARTICIPOU ou ORGANIZOU).
     * Uma pessoa pode ter os dois papéis simultaneamente no mesmo evento.
     */
    public void adicionarParticipacao(String pessoaId, String eventoId, Papel papel) throws SQLException {
        pessoaId = normalizarTexto(pessoaId);
        eventoId = normalizarTexto(eventoId);
        validarRelacionamento(pessoaId, eventoId, papel);
        neo4jRepo.adicionarRelacionamento(pessoaId, eventoId, papel);
        sqlRepo.upsertParticipacao(pessoaId, eventoId, papel);
    }

    public void removerParticipacao(String pessoaId, String eventoId, Papel papel) throws SQLException {
        pessoaId = normalizarTexto(pessoaId);
        eventoId = normalizarTexto(eventoId);
        validarRelacionamento(pessoaId, eventoId, papel);
        neo4jRepo.removerRelacionamento(pessoaId, eventoId, papel);
        sqlRepo.deletarParticipacao(pessoaId, eventoId, papel);
    }

    // ==================== REQUISITO 2: Consultas Neo4j ====================

    /** Apenas ouvintes (PARTICIPOU, sem ORGANIZOU). */
    public List<String> consultarParticipantesOuvintes(String eventoId) {
        return neo4jRepo.consultarSomenteParticipantes(eventoId);
    }

    /** Apenas organizadores. */
    public List<String> consultarOrganizadores(String eventoId) {
        return neo4jRepo.consultarOrganizadores(eventoId);
    }

    /** Organizadores que também participam como ouvintes. */
    public List<String> consultarOrganizadoresETambemParticipantes(String eventoId) {
        return neo4jRepo.consultarOrganizadoresETambemParticipantes(eventoId);
    }

    /** Todos no evento, com papel. */
    public List<Map<String, String>> consultarTodosComPapel(String eventoId) {
        return neo4jRepo.consultarTodosComPapel(eventoId);
    }

    // ==================== REQUISITO 3: Migração ====================

    /**
     * Migra participante → organizador (remove PARTICIPOU, cria ORGANIZOU).
     * @return true se a migração ocorreu, false se a pessoa não era participante.
     */
    public boolean migrarParaOrganizador(String pessoaId, String eventoId) throws SQLException {
        pessoaId = normalizarTexto(pessoaId);
        eventoId = normalizarTexto(eventoId);
        validarRelacionamento(pessoaId, eventoId, Papel.PARTICIPANTE);
        boolean ok = neo4jRepo.migrarParticipanteParaOrganizador(pessoaId, eventoId);
        if (ok) {
            sqlRepo.deletarParticipacao(pessoaId, eventoId, Papel.PARTICIPANTE);
            sqlRepo.upsertParticipacao(pessoaId, eventoId, Papel.ORGANIZADOR);
        }
        return ok;
    }

    /**
     * Adiciona papel ORGANIZADOR sem remover PARTICIPOU
     * (a pessoa fica com os dois papéis simultaneamente).
     */
    public void adicionarPapelOrganizador(String pessoaId, String eventoId) throws SQLException {
        pessoaId = normalizarTexto(pessoaId);
        eventoId = normalizarTexto(eventoId);
        validarRelacionamento(pessoaId, eventoId, Papel.ORGANIZADOR);
        neo4jRepo.adicionarPapelOrganizador(pessoaId, eventoId);
        sqlRepo.upsertParticipacao(pessoaId, eventoId, Papel.ORGANIZADOR);
    }

    // ==================== REQUISITO 4: Filtros dinâmicos MongoDB ====================

    public List<Evento> filtrarEventos(String local, String data, String palavraChave) {
        return mongoRepo.filtrarEventos(local, data, palavraChave);
    }

    // ==================== ESTATÍSTICAS PARA DASHBOARD ====================

    public long totalEventos() { return mongoRepo.contarEventos(); }
    public long totalParticipantes() { return mongoRepo.contarParticipantes(); }
    public long totalOrganizadores() { return neo4jRepo.contarOrganizadores(); }
    public int totalParticipacoesSql() throws SQLException { return sqlRepo.contarParticipacoes(); }

    private void validarEvento(Evento e) {
        if (e == null) {
            throw new IllegalArgumentException("Evento inválido.");
        }

        String nome = normalizarTexto(e.getNome());
        if (nome.length() < 3) {
            throw new IllegalArgumentException("Nome do evento deve ter pelo menos 3 caracteres.");
        }

        String data = normalizarTexto(e.getData());
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Data do evento é obrigatória.");
        }
        try {
            LocalDate.parse(data);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data do evento deve ser uma data válida no formato AAAA-MM-DD.");
        }

        String local = normalizarTexto(e.getLocal());
        if (local.length() < 2) {
            throw new IllegalArgumentException("Local do evento deve ter pelo menos 2 caracteres.");
        }

        e.setNome(nome);
        e.setData(data);
        e.setLocal(local);
        e.setDescricao(normalizarTexto(e.getDescricao()));
        e.setPalavrasChave(normalizarPalavrasChave(e.getPalavrasChave()));
    }

    private void validarParticipante(Participante p) {
        if (p == null) {
            throw new IllegalArgumentException("Participante inválido.");
        }

        String nome = normalizarTexto(p.getNome());
        if (nome.length() < 2) {
            throw new IllegalArgumentException("Nome do participante deve ter pelo menos 2 caracteres.");
        }

        String email = normalizarTexto(p.getEmail()).toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("E-mail do participante é obrigatório.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("E-mail do participante deve ter um formato válido.");
        }

        p.setNome(nome);
        p.setEmail(email);
    }

    private void validarRelacionamento(String pessoaId, String eventoId, Papel papel) {
        if (isCampoRelacaoVazio(eventoId, "ID do Evento")) {
            throw new IllegalArgumentException("Informe o ID do Evento.");
        }
        if (isCampoRelacaoVazio(pessoaId, "ID da Pessoa")) {
            throw new IllegalArgumentException("Informe o ID da Pessoa.");
        }
        if (papel == null) {
            throw new IllegalArgumentException("Papel é obrigatório.");
        }
        if (mongoRepo.buscarEventoPorId(eventoId) == null) {
            throw new IllegalArgumentException("Evento não encontrado para o ID informado.");
        }
        if (mongoRepo.buscarParticipantePorId(pessoaId) == null) {
            throw new IllegalArgumentException("Participante não encontrado para o ID informado.");
        }
    }

    private List<String> normalizarPalavrasChave(List<String> palavras) {
        Map<String, String> unicas = new LinkedHashMap<>();
        if (palavras != null) {
            for (String palavra : palavras) {
                String normalizada = normalizarTexto(palavra);
                if (!normalizada.isEmpty()) {
                    unicas.putIfAbsent(normalizada.toLowerCase(Locale.ROOT), normalizada);
                }
            }
        }
        return new ArrayList<>(unicas.values());
    }

    private boolean isCampoRelacaoVazio(String valor, String placeholder) {
        String normalizado = normalizarTexto(valor);
        return normalizado.isEmpty() || normalizado.equals(placeholder);
    }

    private String normalizarTexto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
