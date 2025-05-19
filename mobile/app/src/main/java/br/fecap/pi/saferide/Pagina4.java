package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

public class Pagina4 extends AppCompatActivity {

    private TextView resultadoView;
    private MaterialButton buttonVoltarMenu;
    private Usuario usuarioLogado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagina4);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        resultadoView = findViewById(R.id.textResultado);
        buttonVoltarMenu = findViewById(R.id.buttonVoltarMenu);

        String resultadoTemperamento = getIntent().getStringExtra("resultado");

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

        if (resultadoTemperamento != null) {
            resultadoView.setText(resultadoTemperamento);
        } else {
            resultadoView.setText("Resultado do temperamento não disponível.");
        }

        if (usuarioLogado == null && buttonVoltarMenu != null) {
            Toast.makeText(this, "Erro: Dados do usuário não carregados.", Toast.LENGTH_SHORT).show();
            buttonVoltarMenu.setText("Voltar ao Login");
            buttonVoltarMenu.setOnClickListener(v -> {
                Intent intent = new Intent(Pagina4.this, Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
            return;
        }

        if(buttonVoltarMenu != null){
            buttonVoltarMenu.setOnClickListener(this::voltarInicio);
        }
    }

    public void voltarInicio(View view) {
        voltarParaMenuCorreto();
    }

    private void voltarParaMenuCorreto() {
        if (usuarioLogado == null) {
            Intent intent = new Intent(this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Intent intent;
        String tipoConta = usuarioLogado.getTipoConta();
        String genero = usuarioLogado.getGenero();

        if ("Prefiro não identificar".equalsIgnoreCase(genero)) {
            intent = new Intent(this, Menu.class);
        } else if ("Motorista".equalsIgnoreCase(tipoConta)) {
            intent = new Intent(this, MenuRider.class);
        } else {
            intent = new Intent(this, Menu.class);
        }
        intent.putExtra("usuario", usuarioLogado);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }
}