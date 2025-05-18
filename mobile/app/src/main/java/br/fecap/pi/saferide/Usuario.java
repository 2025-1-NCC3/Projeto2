package br.fecap.pi.saferide;

import com.google.gson.Gson;

import java.io.Serializable;

public class Usuario implements Serializable {
    private int id; // Adicionando campo id
    private String name;
    private String surname;
    private String email;
    private String number;
    private String genero;
    private String tipoConta;
    private String cpf;
    private String cnh;
    private Carro carro;
    private String password;
    private String salt;

    // Construtor
    public Usuario(String name, String surname, String email, String number) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.number = number;
    }

    // Getter e Setter para o campo id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Outros Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getTipoConta() { return tipoConta; }
    public void setTipoConta(String tipoConta) { this.tipoConta = tipoConta; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }

    public Carro getCarro() { return carro; }
    public void setCarro(Carro carro) { this.carro = carro; }

    // Métodos para o campo password
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getSalt(){ return salt; }
    public void setSalt(String salt){ this.salt = salt; }
    public String toJson(){
        return new Gson().toJson(this);
    }
    public static Usuario fromJson(String json) {
        return new Gson().fromJson(json, Usuario.class);
    }
}