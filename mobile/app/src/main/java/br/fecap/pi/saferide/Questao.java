package br.fecap.pi.saferide;

import java.io.Serializable;

public class Questao implements Serializable {
    private int id;
    private String text;

    public Questao(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}