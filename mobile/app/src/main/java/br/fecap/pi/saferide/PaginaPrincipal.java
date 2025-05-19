package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;

public class PaginaPrincipal extends AppCompatActivity {

    private static final String TAG = "PaginaPrincipal";
    private CheckBox checkBoxPrivacy;
    private MaterialButton btnIniciar, btnPolitica;
    private Usuario usuarioLogado;
    private ArrayList<Questao> listaDePerguntas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pagina_principal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent().hasExtra("usuario")) {
            usuarioLogado = (Usuario) getIntent().getSerializableExtra("usuario");
        }
        if (usuarioLogado == null) {
            SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
            String usuarioJson = prefs.getString(Login.USER_DETAILS_KEY, null);
            if (usuarioJson != null) {
                usuarioLogado = new Gson().fromJson(usuarioJson, Usuario.class);
            }
        }

        if (usuarioLogado == null) {
            Toast.makeText(this, "Erro: Sessão não encontrada. Faça login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        checkBoxPrivacy = findViewById(R.id.checkBoxPrivacy);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnPolitica = findViewById(R.id.btnPolitica);

        carregarPerguntasLocais();
        if(btnIniciar != null) btnIniciar.setEnabled(listaDePerguntas != null && !listaDePerguntas.isEmpty());


        if(btnPolitica != null) btnPolitica.setOnClickListener(this::abrirPolitica);
        if(btnIniciar != null) btnIniciar.setOnClickListener(this::seguinte);
    }

    private void carregarPerguntasLocais(){
        listaDePerguntas = new ArrayList<>();
        listaDePerguntas.add(new Questao(1, getString(R.string.textPE1)));
        listaDePerguntas.add(new Questao(2, getString(R.string.textPE2)));
        listaDePerguntas.add(new Questao(3, getString(R.string.textPE3)));
        listaDePerguntas.add(new Questao(4, getString(R.string.textPE4)));
        listaDePerguntas.add(new Questao(5, getString(R.string.textPE5)));
        listaDePerguntas.add(new Questao(6, getString(R.string.textPN6)));
        listaDePerguntas.add(new Questao(7, getString(R.string.textPN7)));
        listaDePerguntas.add(new Questao(8, getString(R.string.textPN8)));
        listaDePerguntas.add(new Questao(9, getString(R.string.textPN9)));
        listaDePerguntas.add(new Questao(10, getString(R.string.textPN10)));
        listaDePerguntas.add(new Questao(11, getString(R.string.textPP11)));
        listaDePerguntas.add(new Questao(12, getString(R.string.textPP12)));
        listaDePerguntas.add(new Questao(13, getString(R.string.textPP13)));
        listaDePerguntas.add(new Questao(14, getString(R.string.textPP14)));
        listaDePerguntas.add(new Questao(15, getString(R.string.textPP15)));
        Log.d(TAG, "Perguntas locais carregadas: " + listaDePerguntas.size());
    }

    public void abrirPolitica(View view) {
        Intent intent = new Intent(this, PoliticaPrivacidade.class);
        startActivity(intent);
    }

    public void seguinte(View view) {
        if (!checkBoxPrivacy.isChecked()) {
            Toast.makeText(this, "Você deve aceitar a Política de Privacidade para continuar.", Toast.LENGTH_LONG).show();
        } else if (listaDePerguntas == null || listaDePerguntas.isEmpty()) {
            Toast.makeText(this, "Erro ao carregar perguntas.", Toast.LENGTH_SHORT).show();
        } else {
            Intent mudarTela = new Intent(getApplicationContext(), MainActivity.class);
            mudarTela.putExtra("usuario_logado", usuarioLogado);
            mudarTela.putExtra("lista_perguntas", listaDePerguntas);
            mudarTela.putExtra("respostas_formulario_obj", new RespostasFormulario());
            startActivity(mudarTela);
        }
    }
}