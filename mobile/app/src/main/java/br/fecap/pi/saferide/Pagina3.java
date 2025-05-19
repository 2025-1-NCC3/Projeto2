package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Pagina3 extends AppCompatActivity {

    private static final String TAG = "Pagina3Survey";
    private Usuario usuarioLogado;
    private ArrayList<Questao> todasAsPerguntas;
    private RespostasFormulario respostasFormulario;

    private TextView textPergunta11, textPergunta12, textPergunta13, textPergunta14, textPergunta15;
    private EditText editResposta11, editResposta12, editResposta13, editResposta14, editResposta15;
    private MaterialButton buttonAnalisar, buttonAnteriorPag3;
    private ProgressBar surveySubmitProgress; // Embora não usemos para chamada de rede, pode ser útil para feedback

    private final int INDICE_INICIAL_P3 = 10;
    private final int QTD_PERGUNTAS_P3 = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagina3);

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
            Toast.makeText(this, "Erro ao carregar dados do questionário (P3).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        textPergunta11 = findViewById(R.id.textPergunta11);
        editResposta11 = findViewById(R.id.textResposta11);
        textPergunta12 = findViewById(R.id.textPergunta12);
        editResposta12 = findViewById(R.id.textResposta12);
        textPergunta13 = findViewById(R.id.textPergunta13);
        editResposta13 = findViewById(R.id.textResposta13);
        textPergunta14 = findViewById(R.id.textPergunta14);
        editResposta14 = findViewById(R.id.textResposta14);
        textPergunta15 = findViewById(R.id.textPergunta15);
        editResposta15 = findViewById(R.id.textResposta15);

        buttonAnalisar = findViewById(R.id.buttonNext);
        buttonAnteriorPag3 = findViewById(R.id.buttonPrevious);

        popularPerguntasP3();

        buttonAnalisar.setOnClickListener(this::analisar);
        if(buttonAnteriorPag3 != null) buttonAnteriorPag3.setOnClickListener(this::anterior);
    }

    private void popularPerguntasP3() {
        TextView[] textViewsPerguntas = {textPergunta11, textPergunta12, textPergunta13, textPergunta14, textPergunta15};
        if (todasAsPerguntas.size() >= INDICE_INICIAL_P3 + QTD_PERGUNTAS_P3) {
            for (int i = 0; i < QTD_PERGUNTAS_P3; i++) {
                if(textViewsPerguntas[i] != null && (INDICE_INICIAL_P3 + i) < todasAsPerguntas.size()) {
                    textViewsPerguntas[i].setText(todasAsPerguntas.get(INDICE_INICIAL_P3 + i).getText());
                }
            }
        } else {
            Toast.makeText(this, "Não há perguntas suficientes para esta página (P3).", Toast.LENGTH_SHORT).show();
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

    private boolean coletarRespostasDaPaginaAtual() {
        String resp11 = editResposta11.getText().toString().trim().toLowerCase();
        String resp12 = editResposta12.getText().toString().trim().toLowerCase();
        String resp13 = editResposta13.getText().toString().trim().toLowerCase();
        String resp14 = editResposta14.getText().toString().trim().toLowerCase();
        String resp15 = editResposta15.getText().toString().trim().toLowerCase();

        if(!validarRespostas(resp11, resp12, resp13, resp14, resp15)){
            return false;
        }

        if(todasAsPerguntas.size() >= INDICE_INICIAL_P3 + QTD_PERGUNTAS_P3){
            respostasFormulario.adicionarResposta(resp11, "psicoticismo", todasAsPerguntas.get(INDICE_INICIAL_P3 + 0).getId());
            respostasFormulario.adicionarResposta(resp12, "psicoticismo", todasAsPerguntas.get(INDICE_INICIAL_P3 + 1).getId());
            respostasFormulario.adicionarResposta(resp13, "psicoticismo", todasAsPerguntas.get(INDICE_INICIAL_P3 + 2).getId());
            respostasFormulario.adicionarResposta(resp14, "psicoticismo", todasAsPerguntas.get(INDICE_INICIAL_P3 + 3).getId());
            respostasFormulario.adicionarResposta(resp15, "psicoticismo", todasAsPerguntas.get(INDICE_INICIAL_P3 + 4).getId());
        }
        return true;
    }

    public void analisar(View view) {
        if (!coletarRespostasDaPaginaAtual()) {
            return;
        }

        Log.d(TAG, "Todas as respostas coletadas para análise local e envio (se fosse o caso).");
        Log.d(TAG, "Total de respostas no objeto RespostasFormulario para servidor: " + respostasFormulario.getRespostasParaServidor().size());

        if (surveySubmitProgress != null) surveySubmitProgress.setVisibility(View.VISIBLE);
        buttonAnalisar.setEnabled(false);
        if(buttonAnteriorPag3 != null) buttonAnteriorPag3.setEnabled(false);

        String temperamento = respostasFormulario.determinarTemperamento();

        Toast.makeText(Pagina3.this, "Análise concluída localmente.", Toast.LENGTH_LONG).show();
        if (surveySubmitProgress != null) surveySubmitProgress.setVisibility(View.GONE);
        buttonAnalisar.setEnabled(true);
        if(buttonAnteriorPag3 != null) buttonAnteriorPag3.setEnabled(true);

        Intent intent = new Intent(Pagina3.this, Pagina4.class);
        intent.putExtra("resultado", temperamento);
        intent.putExtra("usuario", usuarioLogado);
        startActivity(intent);
        finishAffinity();
    }

    public void anterior(View view) {
        finish();
    }
}