package com.events.ui;

import com.events.config.MongoDBConfig;
import com.events.config.Neo4jConfig;
import com.events.config.PostgreSQLConfig;
import com.events.export.JsonExporter;
import com.events.export.SqlExporter;
import com.events.model.Evento;
import com.events.model.Papel;
import com.events.model.Participante;
import com.events.service.EventoService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard principal em Swing (Requisito 1).
 *
 * Abas:
 *   1. Visão Geral      → estatísticas das 3 bases
 *   2. Eventos          → CRUD + filtros dinâmicos MongoDB
 *   3. Participantes    → CRUD
 *   4. Relacionamentos  → Neo4j: consultas + migração
 *   5. Exportações      → JSON + SQL
 */
public class Dashboard extends JFrame {

    private static final Color COR_PRIMARIA  = new Color(37, 99, 235);
    private static final Color COR_FUNDO     = new Color(248, 250, 252);
    private static final Color COR_CARD      = Color.WHITE;
    private static final Font  FONTE_TITULO  = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font  FONTE_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONTE_BOTAO   = new Font("Segoe UI", Font.BOLD, 12);
    private static final String PLACEHOLDER_EVENTO_ID = "ID do Evento";
    private static final String PLACEHOLDER_PESSOA_ID = "ID da Pessoa";

    private final EventoService service;
    private JTabbedPane tabs;

    // ---- Componentes da aba Visão Geral ----
    private JLabel lblTotalEventos, lblTotalParticipantes, lblTotalOrganizadores, lblTotalParticipacoesSql;

    // ---- Componentes da aba Eventos ----
    private DefaultTableModel modelEventos;
    private JTextField tfFiltroLocal, tfFiltroData, tfFiltroPalavra;

    // ---- Componentes da aba Participantes ----
    private DefaultTableModel modelParticipantes;

    // ---- Componentes da aba Relacionamentos ----
    private JTextArea areaRelacionamentos;
    private JTextField tfRelEventoId, tfRelPessoaId;
    private JComboBox<String> cbPapel;

    // ---- Componentes da aba Exportação ----
    private JTextField tfExportCaminho;
    private JTextArea  areaLog;

    public Dashboard() {
        super("Sistema de Gerenciamento de Eventos");
        this.service = new EventoService();
        buildUI();
    }

    // ================================================================
    // CONSTRUÇÃO DA UI
    // ================================================================

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COR_FUNDO);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        header.setBackground(COR_PRIMARIA);
        JLabel titulo = new JLabel("Sistema de Gerenciamento de Eventos");
        titulo.setFont(FONTE_TITULO);
        titulo.setForeground(Color.WHITE);
        header.add(titulo);

        // Abas
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("Visão Geral",     buildAbaVisaoGeral());
        tabs.addTab("Eventos",          buildAbaEventos());
        tabs.addTab("Participantes",    buildAbaParticipantes());
        tabs.addTab("Relacionamentos",  buildAbaRelacionamentos());
        tabs.addTab("Exportações",      buildAbaExportacoes());

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(tabs,   BorderLayout.CENTER);

        // Carrega dados iniciais
        refreshVisaoGeral();
        refreshTabelaEventos(null, null, null);
        refreshTabelaParticipantes();
    }

    // ================================================================
    // ABA 1: VISÃO GERAL
    // ================================================================

    private JPanel buildAbaVisaoGeral() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(COR_FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.BOTH;

        // Título
        JLabel sub = new JLabel("Estatísticas combinadas: MongoDB, Neo4j e PostgreSQL");
        sub.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        sub.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        p.add(sub, gbc);

        // Cards
        lblTotalEventos       = new JLabel("...");
        lblTotalParticipantes = new JLabel("...");
        lblTotalOrganizadores = new JLabel("...");
        lblTotalParticipacoesSql = new JLabel("...");

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; p.add(buildCard("Eventos (MongoDB)",      lblTotalEventos,      new Color(239,246,255)), gbc);
        gbc.gridx = 1; p.add(buildCard("Participantes (MongoDB)", lblTotalParticipantes, new Color(240,253,244)), gbc);
        gbc.gridx = 2; p.add(buildCard("Organizadores (Neo4j)", lblTotalOrganizadores, new Color(255,247,237)), gbc);
        gbc.gridx = 3; p.add(buildCard("Participações (PostgreSQL)", lblTotalParticipacoesSql, new Color(245,243,255)), gbc);

        // Botão atualizar
        JButton btnRefresh = estilizarBotao(new JButton("Atualizar Estatísticas"), COR_PRIMARIA);
        btnRefresh.addActionListener(e -> refreshVisaoGeral());
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE;
        p.add(btnRefresh, gbc);

        return p;
    }

    private JPanel buildCard(String titulo, JLabel valorLabel, Color bgColor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 8));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209,213,219)),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));
        card.setPreferredSize(new Dimension(220, 110));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valorLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valorLabel.setForeground(COR_PRIMARIA);
        valorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(lblTit);
        card.add(valorLabel);
        return card;
    }

    private void refreshVisaoGeral() {
        try {
            lblTotalEventos.setText(String.valueOf(service.totalEventos()));
            lblTotalParticipantes.setText(String.valueOf(service.totalParticipantes()));
            lblTotalOrganizadores.setText(String.valueOf(service.totalOrganizadores()));
            lblTotalParticipacoesSql.setText(String.valueOf(service.totalParticipacoesSql()));
        } catch (Exception ex) {
            showError("Erro ao carregar estatísticas: " + ex.getMessage());
        }
    }

    // ================================================================
    // ABA 2: EVENTOS
    // ================================================================

    private JPanel buildAbaEventos() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(COR_FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Painel de filtros (Requisito 4)
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filtros.setBackground(COR_CARD);
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros Dinâmicos (MongoDB)"));

        tfFiltroLocal  = new JTextField(12); addPlaceholder(tfFiltroLocal,  "Local...");
        tfFiltroData   = new JTextField(10); addPlaceholder(tfFiltroData,   "Data (AAAA-MM-DD)...");
        tfFiltroPalavra= new JTextField(12); addPlaceholder(tfFiltroPalavra,"Palavra-chave...");

        JButton btnFiltrar = estilizarBotao(new JButton("Filtrar"), COR_PRIMARIA);
        JButton btnLimpar  = estilizarBotao(new JButton("Limpar"), new Color(107, 114, 128));

        btnFiltrar.addActionListener(e -> refreshTabelaEventos(
            tfFiltroLocal.getText().trim(),
            tfFiltroData.getText().trim(),
            tfFiltroPalavra.getText().trim()
        ));
        btnLimpar.addActionListener(e -> {
            tfFiltroLocal.setText(""); tfFiltroData.setText(""); tfFiltroPalavra.setText("");
            refreshTabelaEventos(null, null, null);
        });

        filtros.add(new JLabel("Local:")); filtros.add(tfFiltroLocal);
        filtros.add(new JLabel("Data:"));  filtros.add(tfFiltroData);
        filtros.add(new JLabel("Palavra:")); filtros.add(tfFiltroPalavra);
        filtros.add(btnFiltrar); filtros.add(btnLimpar);

        // Tabela
        String[] colunas = {"ID", "Nome", "Data", "Local", "Descrição", "Palavras-chave"};
        modelEventos = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modelEventos);
        tabela.setRowHeight(26);
        tabela.setFont(FONTE_LABEL);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setMaxWidth(200);

        // Botões CRUD
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        botoes.setBackground(COR_FUNDO);
        JButton btnNovo    = estilizarBotao(new JButton("Novo Evento"),    new Color(22, 163, 74));
        JButton btnEditar  = estilizarBotao(new JButton("Editar"),          COR_PRIMARIA);
        JButton btnDeletar = estilizarBotao(new JButton("Deletar"),         new Color(220, 38, 38));
        JButton btnRefresh = estilizarBotao(new JButton("Atualizar"),       new Color(107, 114, 128));

        btnNovo.addActionListener(e -> dialogNovoEvento());
        btnEditar.addActionListener(e -> dialogEditarEvento(tabela));
        btnDeletar.addActionListener(e -> deletarEvento(tabela));
        btnRefresh.addActionListener(e -> refreshTabelaEventos(null, null, null));

        botoes.add(btnNovo); botoes.add(btnEditar);
        botoes.add(btnDeletar); botoes.add(btnRefresh);

        p.add(filtros,                      BorderLayout.NORTH);
        p.add(new JScrollPane(tabela),      BorderLayout.CENTER);
        p.add(botoes,                       BorderLayout.SOUTH);
        return p;
    }

    private void refreshTabelaEventos(String local, String data, String palavra) {
        modelEventos.setRowCount(0);
        try {
            List<Evento> lista = service.filtrarEventos(
                (local  != null && !local.isBlank())  ? local  : null,
                (data   != null && !data.isBlank())   ? data   : null,
                (palavra!= null && !palavra.isBlank()) ? palavra: null
            );
            for (Evento e : lista) {
                modelEventos.addRow(new Object[]{
                    e.getId(), e.getNome(), e.getData(), e.getLocal(),
                    e.getDescricao(), String.join(", ", e.getPalavrasChave())
                });
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void dialogNovoEvento() {
        JTextField tfNome  = new JTextField(20);
        JTextField tfData  = new JTextField(10);
        JTextField tfLocal = new JTextField(20);
        JTextField tfDesc  = new JTextField(25);
        JTextField tfKW    = new JTextField(25);

        JPanel form = buildForm(
            new String[]{"Nome:", "Data (AAAA-MM-DD):", "Local:", "Descrição:", "Palavras-chave (vírgula):"},
            new JTextField[]{tfNome, tfData, tfLocal, tfDesc, tfKW}
        );

        if (JOptionPane.showConfirmDialog(this, form, "Novo Evento",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Evento e = new Evento(
                UUID.randomUUID().toString().substring(0, 8),
                tfNome.getText(), tfData.getText(), tfLocal.getText(), tfDesc.getText()
            );
            for (String kw : tfKW.getText().split(",")) {
                if (!kw.isBlank()) e.getPalavrasChave().add(kw.trim());
            }
            try {
                service.criarEvento(e);
                refreshTabelaEventos(null, null, null);
                refreshVisaoGeral();
                showInfo("Evento criado com sucesso!\nID: " + e.getId());
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    private void dialogEditarEvento(JTable tabela) {
        int row = tabela.getSelectedRow();
        if (row < 0) { showInfo("Selecione um evento para editar."); return; }

        String id = (String) modelEventos.getValueAt(row, 0);
        Evento e  = service.buscarEvento(id);
        if (e == null) { showError("Evento não encontrado."); return; }

        JTextField tfNome  = new JTextField(e.getNome(),  20);
        JTextField tfData  = new JTextField(e.getData(),  10);
        JTextField tfLocal = new JTextField(e.getLocal(), 20);
        JTextField tfDesc  = new JTextField(e.getDescricao(), 25);
        JTextField tfKW    = new JTextField(String.join(",", e.getPalavrasChave()), 25);

        JPanel form = buildForm(
            new String[]{"Nome:", "Data:", "Local:", "Descrição:", "Palavras-chave:"},
            new JTextField[]{tfNome, tfData, tfLocal, tfDesc, tfKW}
        );

        if (JOptionPane.showConfirmDialog(this, form, "Editar Evento",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            e.setNome(tfNome.getText()); e.setData(tfData.getText());
            e.setLocal(tfLocal.getText()); e.setDescricao(tfDesc.getText());
            e.getPalavrasChave().clear();
            for (String kw : tfKW.getText().split(","))
                if (!kw.isBlank()) e.getPalavrasChave().add(kw.trim());
            try {
                service.atualizarEvento(e);
                refreshTabelaEventos(null, null, null);
                showInfo("Evento atualizado.");
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    private void deletarEvento(JTable tabela) {
        int row = tabela.getSelectedRow();
        if (row < 0) { showInfo("Selecione um evento para deletar."); return; }
        String id   = (String) modelEventos.getValueAt(row, 0);
        String nome = (String) modelEventos.getValueAt(row, 1);

        if (JOptionPane.showConfirmDialog(this, "Deletar \"" + nome + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                service.deletarEvento(id);
                refreshTabelaEventos(null, null, null);
                refreshVisaoGeral();
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    // ================================================================
    // ABA 3: PARTICIPANTES
    // ================================================================

    private JPanel buildAbaParticipantes() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(COR_FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] colunas = {"ID", "Nome", "E-mail"};
        modelParticipantes = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modelParticipantes);
        tabela.setRowHeight(26);
        tabela.setFont(FONTE_LABEL);
        tabela.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        botoes.setBackground(COR_FUNDO);
        JButton btnNovo    = estilizarBotao(new JButton("Novo Participante"), new Color(22, 163, 74));
        JButton btnEditar  = estilizarBotao(new JButton("Editar"),             COR_PRIMARIA);
        JButton btnDeletar = estilizarBotao(new JButton("Deletar"),            new Color(220, 38, 38));
        JButton btnRefresh = estilizarBotao(new JButton("Atualizar"),          new Color(107, 114, 128));

        btnNovo.addActionListener(e -> dialogNovoParticipante());
        btnEditar.addActionListener(e -> dialogEditarParticipante(tabela));
        btnDeletar.addActionListener(e -> deletarParticipante(tabela));
        btnRefresh.addActionListener(e -> refreshTabelaParticipantes());

        botoes.add(btnNovo); botoes.add(btnEditar);
        botoes.add(btnDeletar); botoes.add(btnRefresh);

        p.add(new JScrollPane(tabela), BorderLayout.CENTER);
        p.add(botoes,                  BorderLayout.SOUTH);
        return p;
    }

    private void refreshTabelaParticipantes() {
        modelParticipantes.setRowCount(0);
        try {
            for (Participante pt : service.listarParticipantes()) {
                modelParticipantes.addRow(new Object[]{pt.getId(), pt.getNome(), pt.getEmail()});
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void dialogNovoParticipante() {
        JTextField tfNome  = new JTextField(20);
        JTextField tfEmail = new JTextField(25);
        JPanel form = buildForm(new String[]{"Nome:", "E-mail:"}, new JTextField[]{tfNome, tfEmail});

        if (JOptionPane.showConfirmDialog(this, form, "Novo Participante",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Participante pt = new Participante(
                UUID.randomUUID().toString().substring(0, 8),
                tfNome.getText(), tfEmail.getText()
            );
            try {
                service.criarParticipante(pt);
                refreshTabelaParticipantes();
                refreshVisaoGeral();
                showInfo("Participante criado!\nID: " + pt.getId());
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    private void dialogEditarParticipante(JTable tabela) {
        int row = tabela.getSelectedRow();
        if (row < 0) { showInfo("Selecione um participante."); return; }
        String id = (String) modelParticipantes.getValueAt(row, 0);
        Participante pt = service.buscarParticipante(id);
        if (pt == null) { showError("Participante não encontrado."); return; }

        JTextField tfNome  = new JTextField(pt.getNome(), 20);
        JTextField tfEmail = new JTextField(pt.getEmail(), 25);
        JPanel form = buildForm(new String[]{"Nome:", "E-mail:"}, new JTextField[]{tfNome, tfEmail});

        if (JOptionPane.showConfirmDialog(this, form, "Editar Participante",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            pt.setNome(tfNome.getText()); pt.setEmail(tfEmail.getText());
            try { service.atualizarParticipante(pt); refreshTabelaParticipantes(); }
            catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    private void deletarParticipante(JTable tabela) {
        int row = tabela.getSelectedRow();
        if (row < 0) { showInfo("Selecione um participante."); return; }
        String id   = (String) modelParticipantes.getValueAt(row, 0);
        String nome = (String) modelParticipantes.getValueAt(row, 1);

        if (JOptionPane.showConfirmDialog(this, "Deletar \"" + nome + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                service.deletarParticipante(id);
                refreshTabelaParticipantes();
                refreshVisaoGeral();
            } catch (Exception ex) { showError(ex.getMessage()); }
        }
    }

    // ================================================================
    // ABA 4: RELACIONAMENTOS (Neo4j) — Requisitos 2 e 3
    // ================================================================

    private JPanel buildAbaRelacionamentos() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(COR_FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Formulário de operações
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(COR_CARD);
        form.setBorder(BorderFactory.createTitledBorder("Operações Neo4j"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        tfRelEventoId  = new JTextField(16); addPlaceholder(tfRelEventoId,  PLACEHOLDER_EVENTO_ID);
        tfRelPessoaId  = new JTextField(16); addPlaceholder(tfRelPessoaId,  PLACEHOLDER_PESSOA_ID);
        cbPapel        = new JComboBox<>(new String[]{"PARTICIPANTE", "ORGANIZADOR"});
        cbPapel.setFont(FONTE_LABEL);

        gbc.gridx=0; gbc.gridy=0; form.add(new JLabel("ID do Evento:"), gbc);
        gbc.gridx=1; form.add(tfRelEventoId, gbc);
        gbc.gridx=2; form.add(new JLabel("ID da Pessoa:"), gbc);
        gbc.gridx=3; form.add(tfRelPessoaId, gbc);
        gbc.gridx=4; form.add(new JLabel("Papel:"), gbc);
        gbc.gridx=5; form.add(cbPapel, gbc);

        // Botões de ação
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acoes.setBackground(COR_CARD);

        JButton btnAdicionar      = estilizarBotao(new JButton("Adicionar Relação"), new Color(22,163,74));
        JButton btnRemover        = estilizarBotao(new JButton("Remover Relação"),   new Color(220,38,38));
        JButton btnConsultar      = estilizarBotao(new JButton("Consultar Evento"),  COR_PRIMARIA);
        JButton btnMigrar         = estilizarBotao(new JButton("Migrar para Organizador"), new Color(124,58,237));
        JButton btnDuplosPapeis   = estilizarBotao(new JButton("Adicionar organizador sem remover participante"), new Color(180,83,9));

        btnAdicionar.addActionListener(e    -> adicionarRelacionamento());
        btnRemover.addActionListener(e      -> removerRelacionamento());
        btnConsultar.addActionListener(e    -> consultarRelacionamentos());
        btnMigrar.addActionListener(e       -> migrarParaOrganizador());
        btnDuplosPapeis.addActionListener(e -> adicionarPapelOrganizador());

        acoes.add(btnAdicionar); acoes.add(btnRemover); acoes.add(new JSeparator(SwingConstants.VERTICAL));
        acoes.add(btnConsultar); acoes.add(new JSeparator(SwingConstants.VERTICAL));
        acoes.add(btnMigrar); acoes.add(btnDuplosPapeis);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(COR_FUNDO);
        top.add(form, BorderLayout.NORTH);
        top.add(acoes, BorderLayout.SOUTH);

        // Área de resultado
        areaRelacionamentos = new JTextArea();
        areaRelacionamentos.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaRelacionamentos.setEditable(false);
        areaRelacionamentos.setText("Informe o ID do Evento e clique em 'Consultar Evento' para ver os relacionamentos Neo4j.");

        p.add(top,                                    BorderLayout.NORTH);
        p.add(new JScrollPane(areaRelacionamentos),   BorderLayout.CENTER);
        return p;
    }

    private void adicionarRelacionamento() {
        String eventoId = valorCampoRelacionamento(tfRelEventoId, PLACEHOLDER_EVENTO_ID);
        String pessoaId = valorCampoRelacionamento(tfRelPessoaId, PLACEHOLDER_PESSOA_ID);
        Papel papel     = "ORGANIZADOR".equals(cbPapel.getSelectedItem()) ? Papel.ORGANIZADOR : Papel.PARTICIPANTE;

        if (eventoId.isEmpty() || pessoaId.isEmpty()) { showInfo("Preencha ID do Evento e da Pessoa."); return; }
        try {
            service.adicionarParticipacao(pessoaId, eventoId, papel);
            appendLog("Relação " + papel.name() + " adicionada: " + pessoaId + " -> " + eventoId);
            consultarRelacionamentos();
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void removerRelacionamento() {
        String eventoId = valorCampoRelacionamento(tfRelEventoId, PLACEHOLDER_EVENTO_ID);
        String pessoaId = valorCampoRelacionamento(tfRelPessoaId, PLACEHOLDER_PESSOA_ID);
        Papel papel     = "ORGANIZADOR".equals(cbPapel.getSelectedItem()) ? Papel.ORGANIZADOR : Papel.PARTICIPANTE;

        if (eventoId.isEmpty() || pessoaId.isEmpty()) { showInfo("Preencha ID do Evento e da Pessoa."); return; }
        try {
            service.removerParticipacao(pessoaId, eventoId, papel);
            appendLog("Relação " + papel.name() + " removida: " + pessoaId + " -> " + eventoId);
            consultarRelacionamentos();
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void consultarRelacionamentos() {
        String eventoId = valorCampoRelacionamento(tfRelEventoId, PLACEHOLDER_EVENTO_ID);
        if (eventoId.isEmpty()) { showInfo("Informe o ID do Evento."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------------\n");
        sb.append("  Relacionamentos do Evento: ").append(eventoId).append("\n");
        sb.append("----------------------------------------------\n\n");

        List<String> org = service.consultarOrganizadores(eventoId);
        sb.append("ORGANIZADORES (apenas ORGANIZOU):\n");
        if (org.isEmpty()) sb.append("   (nenhum)\n");
        else org.forEach(n -> sb.append("   - ").append(n).append("\n"));

        sb.append("\n");

        List<String> part = service.consultarParticipantesOuvintes(eventoId);
        sb.append("PARTICIPANTES / OUVINTES (apenas PARTICIPOU):\n");
        if (part.isEmpty()) sb.append("   (nenhum)\n");
        else part.forEach(n -> sb.append("   - ").append(n).append("\n"));

        sb.append("\n");

        List<String> duplos = service.consultarOrganizadoresETambemParticipantes(eventoId);
        sb.append("ORGANIZADORES que também PARTICIPAM (duplo papel):\n");
        if (duplos.isEmpty()) sb.append("   (nenhum)\n");
        else duplos.forEach(n -> sb.append("   - ").append(n).append("\n"));

        sb.append("\n");

        List<Map<String, String>> todos = service.consultarTodosComPapel(eventoId);
        sb.append("TODOS (com papel):\n");
        if (todos.isEmpty()) sb.append("   (nenhum)\n");
        else todos.forEach(m -> sb.append("   - ").append(m.get("nome"))
                .append(" [").append(m.get("papel")).append("]\n"));

        areaRelacionamentos.setText(sb.toString());
    }

    private void migrarParaOrganizador() {
        String eventoId = valorCampoRelacionamento(tfRelEventoId, PLACEHOLDER_EVENTO_ID);
        String pessoaId = valorCampoRelacionamento(tfRelPessoaId, PLACEHOLDER_PESSOA_ID);
        if (eventoId.isEmpty() || pessoaId.isEmpty()) { showInfo("Preencha ID do Evento e da Pessoa."); return; }
        try {
            boolean ok = service.migrarParaOrganizador(pessoaId, eventoId);
            if (ok) {
                appendLog("Migração concluída: " + pessoaId + " agora é ORGANIZADOR no evento " + eventoId);
                consultarRelacionamentos();
            } else {
                showInfo("A pessoa não era PARTICIPANTE neste evento. Nenhuma migração realizada.");
            }
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void adicionarPapelOrganizador() {
        String eventoId = valorCampoRelacionamento(tfRelEventoId, PLACEHOLDER_EVENTO_ID);
        String pessoaId = valorCampoRelacionamento(tfRelPessoaId, PLACEHOLDER_PESSOA_ID);
        if (eventoId.isEmpty() || pessoaId.isEmpty()) { showInfo("Preencha ID do Evento e da Pessoa."); return; }
        try {
            service.adicionarPapelOrganizador(pessoaId, eventoId);
            appendLog(pessoaId + " agora é ORGANIZADOR e PARTICIPANTE no evento " + eventoId);
            consultarRelacionamentos();
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void appendLog(String msg) {
        String atual = areaRelacionamentos.getText();
        areaRelacionamentos.setText(msg + "\n" + atual);
    }

    // ================================================================
    // ABA 5: EXPORTAÇÕES — Requisitos 5 e 6
    // ================================================================

    private JPanel buildAbaExportacoes() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(COR_FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel controles = new JPanel(new GridBagLayout());
        controles.setBackground(COR_CARD);
        controles.setBorder(BorderFactory.createTitledBorder("Opções de Exportação"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        tfExportCaminho = new JTextField("exports/", 28);

        gbc.gridx=0; gbc.gridy=0; controles.add(new JLabel("Diretório de saída:"), gbc);
        gbc.gridx=1; gbc.gridwidth=2; controles.add(tfExportCaminho, gbc);

        JButton btnJsonEventos  = estilizarBotao(new JButton("Exportar Eventos para JSON"),      new Color(14,165,233));
        JButton btnJsonPart     = estilizarBotao(new JButton("Exportar Participantes para JSON"),new Color(14,165,233));
        JButton btnJsonTudo     = estilizarBotao(new JButton("Exportar Tudo para JSON"),         new Color(6,182,212));
        JButton btnSqlArquivo   = estilizarBotao(new JButton("Gerar Script SQL"),             new Color(126,34,206));
        JButton btnSqlPostgres  = estilizarBotao(new JButton("Sincronizar para PostgreSQL"),     new Color(37,99,235));

        gbc.gridwidth=1; gbc.gridy=1; gbc.gridx=0; controles.add(btnJsonEventos,  gbc);
        gbc.gridx=1;                                controles.add(btnJsonPart,     gbc);
        gbc.gridx=2;                                controles.add(btnJsonTudo,     gbc);
        gbc.gridy=2; gbc.gridx=0;                  controles.add(btnSqlArquivo,   gbc);
        gbc.gridx=1;                                controles.add(btnSqlPostgres,  gbc);

        btnJsonEventos.addActionListener(e  -> exportarJsonEventos());
        btnJsonPart.addActionListener(e     -> exportarJsonParticipantes());
        btnJsonTudo.addActionListener(e     -> exportarJsonTudo());
        btnSqlArquivo.addActionListener(e   -> gerarScriptSQL());
        btnSqlPostgres.addActionListener(e  -> sincronizarPostgres());

        areaLog = new JTextArea();
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLog.setEditable(false);
        areaLog.setText("Pronto. Use os botões acima para exportar dados.\n");

        p.add(controles,              BorderLayout.NORTH);
        p.add(new JScrollPane(areaLog), BorderLayout.CENTER);
        return p;
    }

    private String caminho() {
        String dir = tfExportCaminho.getText().trim();
        if (!dir.endsWith("/")) dir += "/";
        return dir;
    }

    private void exportarJsonEventos() {
        try {
            List<Evento> eventos = service.listarEventos();
            String path = caminho() + "eventos.json";
            JsonExporter.exportarEventos(eventos, path);
            logExport("Exportado: " + path + " (" + eventos.size() + " eventos)");
        } catch (Exception ex) { logExport("Erro: " + ex.getMessage()); }
    }

    private void exportarJsonParticipantes() {
        try {
            List<Participante> lista = service.listarParticipantes();
            String path = caminho() + "participantes.json";
            JsonExporter.exportarParticipantes(lista, path);
            logExport("Exportado: " + path + " (" + lista.size() + " participantes)");
        } catch (Exception ex) { logExport("Erro: " + ex.getMessage()); }
    }

    private void exportarJsonTudo() {
        try {
            String path = caminho() + "export_completo.json";
            JsonExporter.exportarTudo(service.listarEventos(), service.listarParticipantes(), path);
            logExport("Exportação completa: " + path);
        } catch (Exception ex) { logExport("Erro: " + ex.getMessage()); }
    }

    private void gerarScriptSQL() {
        try {
            String path = caminho() + "dump.sql";
            SqlExporter.gerarArquivoSQL(service.listarEventos(), service.listarParticipantes(), path);
            logExport("Script SQL gerado: " + path);
        } catch (Exception ex) { logExport("Erro: " + ex.getMessage()); }
    }

    private void sincronizarPostgres() {
        try {
            SqlExporter exp = new SqlExporter();
            exp.exportarParaPostgres(service.listarEventos(), service.listarParticipantes());
            refreshVisaoGeral();
            logExport("Sincronização com PostgreSQL concluída.");
        } catch (Exception ex) { logExport("Erro: " + ex.getMessage()); }
    }

    private void logExport(String msg) {
        areaLog.append("[" + java.time.LocalTime.now().withNano(0) + "] " + msg + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // ================================================================
    // UTILITÁRIOS
    // ================================================================

    private JButton estilizarBotao(JButton btn, Color cor) {
        btn.setFont(FONTE_BOTAO);
        btn.setBackground(cor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return btn;
    }

    private JPanel buildForm(String[] labels, JTextField[] fields) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; form.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1; form.add(fields[i], gbc);
        }
        return form;
    }

    private void addPlaceholder(JTextField tf, String placeholder) {
        tf.setForeground(Color.GRAY);
        tf.setText(placeholder);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) { tf.setText(""); tf.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) { tf.setText(placeholder); tf.setForeground(Color.GRAY); }
            }
        });
    }

    private String valorCampoRelacionamento(JTextField tf, String placeholder) {
        String valor = tf.getText().trim();
        return valor.equals(placeholder) ? "" : valor;
    }

    private void showInfo(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro",  JOptionPane.ERROR_MESSAGE); }

    // ================================================================
    // MAIN
    // ================================================================

    public static void main(String[] args) {
        // Inicializa o schema SQL
        try {
            PostgreSQLConfig.initSchema();
        } catch (Exception ex) {
            System.err.println("[AVISO] PostgreSQL indisponível: " + ex.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Dashboard().setVisible(true);
        });

        // Garante fechamento das conexões ao sair
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MongoDBConfig.close();
            Neo4jConfig.close();
            PostgreSQLConfig.close();
        }));
    }
}
