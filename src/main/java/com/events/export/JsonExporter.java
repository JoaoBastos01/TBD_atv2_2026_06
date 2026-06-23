package com.events.export;

import com.events.model.Evento;
import com.events.model.Participante;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exportação JSON (Requisito 5) → integração com sistemas externos.
 *
 * Gera arquivos JSON com todos os dados de eventos e participantes.
 */
public class JsonExporter {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Exporta todos os eventos para um arquivo JSON.
     * @param eventos  lista de eventos do MongoDB
     * @param caminho  caminho do arquivo de saída (ex: "exports/eventos.json")
     */
    public static void exportarEventos(List<Evento> eventos, String caminho) throws IOException {
        File dir = new File(caminho).getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("totalEventos", eventos.size());
        wrapper.put("eventos", eventos);
        wrapper.put("exportadoEm", java.time.LocalDateTime.now().toString());

        mapper.writeValue(new File(caminho), wrapper);
        System.out.println("[JSON] Eventos exportados para: " + caminho);
    }

    /**
     * Exporta todos os participantes para um arquivo JSON.
     */
    public static void exportarParticipantes(List<Participante> participantes, String caminho) throws IOException {
        File dir = new File(caminho).getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("totalParticipantes", participantes.size());
        wrapper.put("participantes", participantes);
        wrapper.put("exportadoEm", java.time.LocalDateTime.now().toString());

        mapper.writeValue(new File(caminho), wrapper);
        System.out.println("[JSON] Participantes exportados para: " + caminho);
    }

    /**
     * Exporta tudo (eventos + participantes) em um único arquivo JSON.
     */
    public static void exportarTudo(List<Evento> eventos, List<Participante> participantes, String caminho)
            throws IOException {
        File dir = new File(caminho).getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();

        Map<String, Object> payload = new HashMap<>();
        payload.put("exportadoEm", java.time.LocalDateTime.now().toString());
        payload.put("totalEventos", eventos.size());
        payload.put("totalParticipantes", participantes.size());
        payload.put("eventos", eventos);
        payload.put("participantes", participantes);

        mapper.writeValue(new File(caminho), payload);
        System.out.println("[JSON] Exportação completa salva em: " + caminho);
    }

    /**
     * Retorna a representação JSON de um único evento como String.
     * Útil para integração via API REST.
     */
    public static String eventoParaJson(Evento evento) throws IOException {
        return mapper.writeValueAsString(evento);
    }

    /**
     * Retorna a representação JSON de um único participante como String.
     */
    public static String participanteParaJson(Participante participante) throws IOException {
        return mapper.writeValueAsString(participante);
    }
}
