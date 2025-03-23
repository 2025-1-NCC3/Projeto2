package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Pagina3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pagina3);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void analisar(View view){
        // Instanciando o objeto que recebrá e armazenará as respostas
        RespostasFormulario respostas = new RespostasFormulario();

        // Referenciando os campos de resposta
        EditText campoResposta11 = findViewById(R.id.textResposta11);
        EditText campoResposta12 = findViewById(R.id.textResposta12);
        EditText campoResposta13 = findViewById(R.id.textResposta13);
        EditText campoResposta14 = findViewById(R.id.textResposta14);
        EditText campoResposta15 = findViewById(R.id.textResposta15);

        // Atribuindo os valores inseridos e passando para LowerCase
        String resp11 = campoResposta11.getText().toString().toLowerCase();
        String resp12 = campoResposta12.getText().toString().toLowerCase();
        String resp13 = campoResposta13.getText().toString().toLowerCase();
        String resp14 = campoResposta14.getText().toString().toLowerCase();
        String resp15 = campoResposta15.getText().toString().toLowerCase();

        // Associando as respostas à categoria e adicionando o valor inserido pelo objeto
        respostas.adicionarResposta(resp11, "psicoticismo");
        respostas.adicionarResposta(resp12, "psicoticismo");
        respostas.adicionarResposta(resp13, "psicoticismo");
        respostas.adicionarResposta(resp14, "psicoticismo");
        respostas.adicionarResposta(resp15, "psicoticismo");

        // Condição para que a pessoa não consiga analisar com resposta vazia
        if((resp11.isEmpty()) || (resp12.isEmpty()) || (resp13.isEmpty()) || (resp14.isEmpty()) || (resp15.isEmpty())){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erro")
                    .setMessage("Todos os Campos Devem Ser Preenchidos")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {

            // Obtém a análise do temperamento
            String temperamento = respostas.determinarTemperamento();

            // Exibir o resultado
            Intent intent = new Intent(this, Pagina4.class);
            intent.putExtra("resultado", temperamento);
            startActivity(intent);
            finish();
        }
    }

    public void anterior(View view){
        Intent voltarTela = new Intent(getApplicationContext(), Pagina2.class);
        startActivity(voltarTela); // Adicionado startActivity para efetivar a mudança de tela
    }
}