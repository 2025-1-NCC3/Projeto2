package br.fecap.pi.saferide;

import java.io.Serializable;

public class RespostasFormulario implements Serializable {
    // Declaração das variáveis
    private int extroversaoSim, extroversaoNao;
    private int neuroticismoSim, neuroticismoNao;
    private int psicoticismoSim, psicoticismoNao;

    // Construtor do objeto
    public RespostasFormulario() {
        this.extroversaoSim = 0;
        this.extroversaoNao = 0;
        this.neuroticismoSim = 0;
        this.neuroticismoNao = 0;
        this.psicoticismoSim = 0;
        this.psicoticismoNao = 0;
    }

    // Método que armazena a "pontuação" das respostas
    public void adicionarResposta(String resposta, String categoria) {
        boolean ehSim = resposta.equalsIgnoreCase("sim");

        switch (categoria) {
            case "extroversao":
                if (ehSim) extroversaoSim++;
                else extroversaoNao++;
                break;
            case "neuroticismo":
                if (ehSim) neuroticismoSim++;
                else neuroticismoNao++;
                break;
            case "psicoticismo":
                if (ehSim) psicoticismoSim++;
                else psicoticismoNao++;
                break;
        }
    }

    // Método para obter a pontuação final de cada categoria
    public int getPontuacao(String categoria) {
        switch (categoria) {
            case "extroversao":
                return extroversaoSim; // Consideramos apenas as respostas "Sim"
            case "neuroticismo":
                return neuroticismoSim;
            case "psicoticismo":
                return psicoticismoSim;
            default:
                return 0;
        }
    }

    // Método para Determinar o Comportamento
    public String determinarTemperamento() {
        int extroversao = getPontuacao("extroversao");
        int neuroticismo = getPontuacao("neuroticismo");

        boolean ehExtrovertido = extroversao >= 3; // 3-5 Extrovertido, 0-2 Introvertido
        boolean ehNeurotico = neuroticismo >= 3;   // 3-5 Neurótico, 0-2 Estável

        if (ehExtrovertido && !ehNeurotico) {
            return "Sanguíneo (Sociável, otimista, animado)";
        } else if (ehExtrovertido && ehNeurotico) {
            return "Colérico (Ativo, impulsivo, agressivo)";
        } else if (!ehExtrovertido && ehNeurotico) {
            return "Melancólico (Pessimista, reservado, ansioso)";
        } else {
            return "Fleumático (Calmo, passivo, confiável)";
        }
    }

    // Atribuição dos valores obtidos às variáveis
    public int getExtroversaoSim() {
        return extroversaoSim;
    }

    public int getExtroversaoNao() {
        return extroversaoNao;
    }

    public int getNeuroticismoSim() {
        return neuroticismoSim;
    }

    public int getNeuroticismoNao() {
        return neuroticismoNao;
    }

    public int getPsicoticismoSim() {
        return psicoticismoSim;
    }

    public int getPsicoticismoNao() {
        return psicoticismoNao;
    }
}

