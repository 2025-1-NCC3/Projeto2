package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Pagina4 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pagina4);

        // Obtendo o resultado da análise passada pela intent
        String resultado = getIntent().getStringExtra("resultado");

        // Referenciando o TextView e exibindo o resultado
        TextView resultadoView = findViewById(R.id.textResultado);
        resultadoView.setText(resultado);
    }


    // Método para voltar à tela inicial
    public void voltarInicio(View view) {
        Intent intent = new Intent(this, PaginaPrincipal.class);
        startActivity(intent);
        finish(); // Finaliza a atividade para não ficar na pilha
    }

}

