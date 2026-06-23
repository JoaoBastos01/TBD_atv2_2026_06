package com.events.model;

/**
 * Representa uma Pessoa (participante/organizador) armazenada no MongoDB.
 */
public class Participante {

    private String id;
    private String nome;
    private String email;

    public Participante() {}

    public Participante(String id, String nome, String email) {
        this.id    = id;
        this.nome  = nome;
        this.email = email;
    }

    // ---- Getters & Setters ----

    public String getId()               { return id; }
    public void   setId(String id)      { this.id = id; }

    public String getNome()             { return nome; }
    public void   setNome(String nome)  { this.nome = nome; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    @Override
    public String toString() {
        return String.format("[%s] %s <%s>", id, nome, email);
    }
}
