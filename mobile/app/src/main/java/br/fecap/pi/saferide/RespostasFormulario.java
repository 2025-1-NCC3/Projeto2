package br.fecap.pi.saferide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RespostasFormulario implements Serializable {
    private int extroversaoSim, extroversaoNao;
    private int neuroticismoSim, neuroticismoNao;
    private int psicoticismoSim, psicoticismoNao;
    private List<RespostaQuestionarioItem> respostasParaServidor;

    public RespostasFormulario() {
        this.extroversaoSim = 0;
        this.extroversaoNao = 0;
        this.neuroticismoSim = 0;
        this.neuroticismoNao = 0;
        this.psicoticismoSim = 0;
        this.psicoticismoNao = 0;
        this.respostasParaServidor = new ArrayList<>();
    }

    public void adicionarResposta(String resposta, String categoria, int questionId) {
        boolean ehSim = resposta.trim().equalsIgnoreCase("sim");
        String respostaNormalizada = ehSim ? "sim" : "não";

        respostasParaServidor.removeIf(item -> item.getQuestionId() == questionId);
        respostasParaServidor.add(new RespostaQuestionarioItem(questionId, respostaNormalizada));

        switch (categoria.toLowerCase()) {
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

    public List<RespostaQuestionarioItem> getRespostasParaServidor() {
        return respostasParaServidor;
    }

    public int getPontuacao(String categoria) {
        switch (categoria.toLowerCase()) {
            case "extroversao": return extroversaoSim;
            case "neuroticismo": return neuroticismoSim;
            case "psicoticismo": return psicoticismoSim;
            default: return 0;
        }
    }

    public String determinarTemperamento() {
        int extroversao = getPontuacao("extroversao");
        int neuroticismo = getPontuacao("neuroticismo");
        boolean ehExtrovertido = extroversao >= 3;
        boolean ehNeurotico = neuroticismo >= 3;

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

    public int getExtroversaoSim() { return extroversaoSim; }
    public int getExtroversaoNao() { return extroversaoNao; }
    public int getNeuroticismoSim() { return neuroticismoSim; }
    public int getNeuroticismoNao() { return neuroticismoNao; }
    public int getPsicoticismoSim() { return psicoticismoSim; }
    public int getPsicoticismoNao() { return psicoticismoNao; }
}