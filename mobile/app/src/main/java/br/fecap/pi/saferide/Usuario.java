package br.fecap.pi.saferide;

import com.google.gson.Gson; // Mantenha Gson se usar para outras serializações locais
import java.io.Serializable;

public class Usuario implements Serializable {
    private int id;
    private String name;
    private String surname; // Seu backend espera 'name', mas pode ser que você use surname em outro lugar
    private String email;
    private String phone; // No seu backend, é 'phone', aqui está 'number'
    private String birthday; // Adicionado para corresponder ao backend
    private String genero;
    private String tipoConta;
    private String cpf;
    private String cnh;
    private Carro carro; // Mantido, útil se o backend retornar info do carro aninhada
    private String password; // Usado temporariamente para coletar a senha do usuário
    private String salt;     // Mantido, mas provavelmente não usado pelo bcrypt do servidor da forma como está

    // Construtor principal que pode ser usado pela API
    public Usuario(String name, String email, String phone, String birthday, String genero, String tipoConta, String cpf) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthday = birthday;
        this.genero = genero;
        this.tipoConta = tipoConta;
        this.cpf = cpf;
    }

    // Construtor mais simples que você tinha, pode ser útil para outros cenários
    public Usuario(String name, String surname, String email, String number) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phone = number; // Mapeando 'number' para 'phone'
    }


    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Mantendo 'getNumber' mas usando 'phone' internamente para consistência com backend
    public String getNumber() { return phone; }
    public void setNumber(String number) { this.phone = number; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSalt() { return salt; } // Getter para salt
    public void setSalt(String salt) { this.salt = salt; } // Setter para salt

    public String toJson(){ return new Gson().toJson(this); }
    public static Usuario fromJson(String json) { return new Gson().fromJson(json, Usuario.class); }
}