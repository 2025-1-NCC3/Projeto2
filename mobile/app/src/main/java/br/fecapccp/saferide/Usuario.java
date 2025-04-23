package br.fecapccp.saferide;

import com.google.gson.Gson;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String name;
    private String surname;
    private String email;
    private String number;
    private String password; // Adicionando o campo password
    private String salt;

    // Construtor
    public Usuario(String name, String surname, String email, String number) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.number = number;
    }

    // Getters e Setters
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