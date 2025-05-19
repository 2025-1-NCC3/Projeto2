package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;

public class Pagina2 extends AppCompatActivity {

    private Usuario usuarioLogado;
    private ArrayList<Questao> todasAsPerguntas;
    private RespostasFormulario respostasFormulario;

    private TextView textPergunta5, textPergunta6, textPergunta7, textPergunta8, textPergunta9, textPergunta10;
    private EditText editResposta5, editResposta6, editResposta7, editResposta8, editResposta9, editResposta10;
    private MaterialButton buttonNext, buttonPrevious;

    private final int INDICE_INICIAL_P2 = 4;
    private final int QTD_PERGUNTAS_P2 = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagina2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent().hasExtra("usuario_logado")) {
            usuarioLogado = (Usuario) getIntent().getSerializableExtra("usuario_logado");
        }
        if (getIntent().hasExtra("lista_perguntas")) {
            todasAsPerguntas = (ArrayList<Questao>) getIntent().getSerializableExtra("lista_perguntas");
        }
        if (getIntent().hasExtra("respostas_formulario_obj")) {
            respostasFormulario = (RespostasFormulario) getIntent().getSerializableExtra("respostas_formulario_obj");
        }

        if (usuarioLogado == null || todasAsPerguntas == null || respostasFormulario == null) {
            Toast.makeText(this, "Erro ao carregar dados do questionário (P2).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        textPergunta5 = findViewById(R.id.textPergunta5);
        editResposta5 = findViewById(R.id.textResposta5);
        textPergunta6 = findViewById(R.id.textPergunta6);
        editResposta6 = findViewById(R.id.textResposta6);
        textPergunta7 = findViewById(R.id.textPergunta7);
        editResposta7 = findViewById(R.id.textResposta7);
        textPergunta8 = findViewById(R.id.textPergunta8);
        editResposta8 = findViewById(R.id.textResposta8);
        textPergunta9 = findViewById(R.id.textPergunta9);
        editResposta9 = findViewById(R.id.textResposta9);
        textPergunta10 = findViewById(R.id.textPergunta10);
        editResposta10 = findViewById(R.id.textResposta10);

        buttonNext = findViewById(R.id.buttonNext);
        buttonPrevious = findViewById(R.id.buttonPrevious);

        popularPerguntasP2();
    }

    private void popularPerguntasP2() {
        TextView[] textViewsPerguntas = {textPergunta5, textPergunta6, textPergunta7, textPergunta8, textPergunta9, textPergunta10};
        if (todasAsPerguntas.size() >= INDICE_INICIAL_P2 + QTD_PERGUNTAS_P2) {
            for (int i = 0; i < QTD_PERGUNTAS_P2; i++) {
                if(textViewsPerguntas[i] != null && (INDICE_INICIAL_P2 + i) < todasAsPerguntas.size()) {
                    textViewsPerguntas[i].setText(todasAsPerguntas.get(INDICE_INICIAL_P2 + i).getText());
                }
            }
        } else {
            Toast.makeText(this, "Não há perguntas suficientes para esta página (P2).", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validarRespostas(String... respostas) {
        for (String resp : respostas) {
            if (resp.isEmpty() || (!resp.equalsIgnoreCase("sim") && !resp.equalsIgnoreCase("não"))) {
                new AlertDialog.Builder(this)
                        .setTitle("Resposta Inválida")
                        .setMessage("Por favor, responda todas as perguntas com 'sim' ou 'não'.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                return false;
            }
        }
        return true;
    }

    public void seguinte(View view) {
        String resp5 = editResposta5.getText().toString().trim().toLowerCase();
        String resp6 = editResposta6.getText().toString().trim().toLowerCase();
        String resp7 = editResposta7.getText().toString().trim().toLowerCase();
        String resp8 = editResposta8.getText().toString().trim().toLowerCase();
        String resp9 = editResposta9.getText().toString().trim().toLowerCase();
        String resp10 = editResposta10.getText().toString().trim().toLowerCase();

        if (!validarRespostas(resp5, resp6, resp7, resp8, resp9, resp10)) {
            return;
        }

        if(todasAsPerguntas.size() >= INDICE_INICIAL_P2 + QTD_PERGUNTAS_P2){
            respostasFormulario.adicionarResposta(resp5, "extroversao", todasAsPerguntas.get(INDICE_INICIAL_P2 + 0).getId());
            respostasFormulario.adicionarResposta(resp6, "neuroticismo", todasAsPerguntas.get(INDICE_INICIAL_P2 + 1).getId());
            respostasFormulario.adicionarResposta(resp7, "neuroticismo", todasAsPerguntas.get(INDICE_INICIAL_P2 + 2).getId());
            respostasFormulario.adicionarResposta(resp8, "neuroticismo", todasAsPerguntas.get(INDICE_INICIAL_P2 + 3).getId());
            respostasFormulario.adicionarResposta(resp9, "neuroticismo", todasAsPerguntas.get(INDICE_INICIAL_P2 + 4).getId());
            respostasFormulario.adicionarResposta(resp10, "neuroticismo", todasAsPerguntas.get(INDICE_INICIAL_P2 + 5).getId());
        }

        Intent mudarTela = new Intent(Pagina2.this, Pagina3.class);
        mudarTela.putExtra("usuario_logado", usuarioLogado);
        mudarTela.putExtra("lista_perguntas", todasAsPerguntas);
        mudarTela.putExtra("respostas_formulario_obj", respostasFormulario);
        startActivity(mudarTela);
    }

    public void anterior(View view) {
        finish();
    }
}