package com.events.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa um Evento armazenado no MongoDB.
 */
public class Evento {

    private String       id;
    private String       nome;
    private String       data;       // formato ISO: "YYYY-MM-DD"
    private String       local;
    private String       descricao;
    private List<String> palavrasChave;

    public Evento() {
        this.palavrasChave = new ArrayList<>();
    }

    public Evento(String id, String nome, String data, String local, String descricao) {
        this.id           = id;
        this.nome         = nome;
        this.data         = data;
        this.local        = local;
        this.descricao    = descricao;
        this.palavrasChave = new ArrayList<>();
    }

    // ---- Getters & Setters ----

    public String getId()                    { return id; }
    public void   setId(String id)           { this.id = id; }

    public String getNome()                  { return nome; }
    public void   setNome(String nome)       { this.nome = nome; }

    public String getData()                  { return data; }
    public void   setData(String data)       { this.data = data; }

    public String getLocal()                 { return local; }
    public void   setLocal(String local)     { this.local = local; }

    public String getDescricao()             { return descricao; }
    public void   setDescricao(String d)     { this.descricao = d; }

    public List<String> getPalavrasChave()               { return palavrasChave; }
    public void setPalavrasChave(List<String> palavras)  { this.palavrasChave = palavras; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s", id, nome, data, local);
    }
}
