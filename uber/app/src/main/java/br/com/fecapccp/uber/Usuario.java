package br.com.fecapccp.uber;

import java.io.Serializable;

public class Usuario implements Serializable {
    private String name;
    private String surname;
    private String email;
    private String number;
    private String password; // Adicionando o campo password

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
}