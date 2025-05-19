package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    private Usuario usuarioLogado;
    private ArrayList<Questao> todasAsPerguntas;
    private RespostasFormulario respostasFormulario;

    private TextView textPergunta1, textPergunta2, textPergunta3, textPergunta4;
    private EditText editResposta1, editResposta2, editResposta3, editResposta4;
    private Button buttonNext;

    private final int INDICE_INICIAL_P1 = 0;
    private final int QTD_PERGUNTAS_P1 = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            Toast.makeText(this, "Erro ao carregar dados do questionário (P1).", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        textPergunta1 = findViewById(R.id.textPergunta1);
        editResposta1 = findViewById(R.id.textResposta1);
        textPergunta2 = findViewById(R.id.textPergunta2);
        editResposta2 = findViewById(R.id.textResposta2);
        textPergunta3 = findViewById(R.id.textPergunta3);
        editResposta3 = findViewById(R.id.textResposta3);
        textPergunta4 = findViewById(R.id.textPergunta4);
        editResposta4 = findViewById(R.id.textResposta4);
        buttonNext = findViewById(R.id.buttonNext);

        popularPerguntasP1();
    }

    private void popularPerguntasP1() {
        TextView[] textViewsPerguntas = {textPergunta1, textPergunta2, textPergunta3, textPergunta4};
        if (todasAsPerguntas.size() >= INDICE_INICIAL_P1 + QTD_PERGUNTAS_P1) {
            for (int i = 0; i < QTD_PERGUNTAS_P1; i++) {
                if (textViewsPerguntas[i] != null && (INDICE_INICIAL_P1 + i) < todasAsPerguntas.size()) {
                    textViewsPerguntas[i].setText(todasAsPerguntas.get(INDICE_INICIAL_P1 + i).getText());
                }
            }
        } else {
            Toast.makeText(this, "Não há perguntas suficientes para esta página.", Toast.LENGTH_SHORT).show();
            if (buttonNext != null) buttonNext.setEnabled(false);
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
        String resp1 = editResposta1.getText().toString().trim().toLowerCase();
        String resp2 = editResposta2.getText().toString().trim().toLowerCase();
        String resp3 = editResposta3.getText().toString().trim().toLowerCase();
        String resp4 = editResposta4.getText().toString().trim().toLowerCase();

        if (!validarRespostas(resp1, resp2, resp3, resp4)) {
            return;
        }

        if(todasAsPerguntas.size() >= INDICE_INICIAL_P1 + QTD_PERGUNTAS_P1) {
            respostasFormulario.adicionarResposta(resp1, "extroversao", todasAsPerguntas.get(INDICE_INICIAL_P1 + 0).getId());
            respostasFormulario.adicionarResposta(resp2, "extroversao", todasAsPerguntas.get(INDICE_INICIAL_P1 + 1).getId());
            respostasFormulario.adicionarResposta(resp3, "extroversao", todasAsPerguntas.get(INDICE_INICIAL_P1 + 2).getId());
            respostasFormulario.adicionarResposta(resp4, "extroversao", todasAsPerguntas.get(INDICE_INICIAL_P1 + 3).getId());
        }

        Intent mudarTela = new Intent(MainActivity.this, Pagina2.class);
        mudarTela.putExtra("usuario_logado", usuarioLogado);
        mudarTela.putExtra("lista_perguntas", todasAsPerguntas);
        mudarTela.putExtra("respostas_formulario_obj", respostasFormulario);
        startActivity(mudarTela);
    }
}