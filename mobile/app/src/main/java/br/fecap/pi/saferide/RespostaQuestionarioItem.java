package br.fecap.pi.saferide;

import java.io.Serializable;

public class RespostaQuestionarioItem implements Serializable {
    private int questionId;
    private String answer;

    public RespostaQuestionarioItem(int questionId, String answer) {
        this.questionId = questionId;
        this.answer = answer;
    }

    public int getQuestionId() { return questionId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}