package br.fecap.pi.saferide;

import java.io.Serializable;

public class Carro implements Serializable {
    private String marca;
    private String modelo;
    private String cor;
    private String ano;
    private String placa;

    public Carro(String marca, String modelo, String cor, String ano, String placa) {
        this.marca = marca;
        this.modelo = modelo;
        this.cor = cor;
        this.ano = ano;
        this.placa = placa;
    }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public String getAno() { return ano; }
    public void setAno(String ano) { this.ano = ano; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
}