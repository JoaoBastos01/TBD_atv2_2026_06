package com.events.model;

/**
 * Representa uma participacao espelhada no PostgreSQL.
 */
public class Participacao {

    private final String participanteId;
    private final String eventoId;
    private final Papel papel;

    public Participacao(String participanteId, String eventoId, Papel papel) {
        this.participanteId = participanteId;
        this.eventoId = eventoId;
        this.papel = papel;
    }

    public String getParticipanteId() {
        return participanteId;
    }

    public String getEventoId() {
        return eventoId;
    }

    public Papel getPapel() {
        return papel;
    }
}
