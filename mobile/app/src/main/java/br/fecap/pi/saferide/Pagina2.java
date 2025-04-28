package br.fecap.pi.saferide;

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

import br.fecap.pi.saferide.R;

public class Pagina2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pagina2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void seguinte(View view){
        // Instanciando o objeto que receberá e armazenará as respostas
        RespostasFormulario respostas = new RespostasFormulario();

        // Referenciando os campos de resposta
        EditText campoResposta5 = findViewById(R.id.textResposta5);
        EditText campoResposta6 = findViewById(R.id.textResposta6);
        EditText campoResposta7 = findViewById(R.id.textResposta7);
        EditText campoResposta8 = findViewById(R.id.textResposta8);
        EditText campoResposta9 = findViewById(R.id.textResposta9);
        EditText campoResposta10 = findViewById(R.id.textResposta10);

        // Atribuindo os valores inseridos e passando para LowerCase
        String resp5 = campoResposta5.getText().toString().toLowerCase();
        String resp6 = campoResposta6.getText().toString().toLowerCase();
        String resp7 = campoResposta7.getText().toString().toLowerCase();
        String resp8 = campoResposta8.getText().toString().toLowerCase();
        String resp9 = campoResposta9.getText().toString().toLowerCase();
        String resp10 = campoResposta10.getText().toString().toLowerCase();

        // Associando as respostas à categoria e adicionando o valor inserido pelo objeto
        respostas.adicionarResposta(resp5, "extroversao");
        respostas.adicionarResposta(resp6, "neuroticismo");
        respostas.adicionarResposta(resp7, "neuroticismo");
        respostas.adicionarResposta(resp8, "neuroticismo");
        respostas.adicionarResposta(resp9, "neuroticismo");
        respostas.adicionarResposta(resp10, "neuroticismo");

        // Condição para que a pessoa não consiga analisar com resposta vazia
        if((resp5.isEmpty()) || (resp6.isEmpty()) || (resp7.isEmpty()) || (resp8.isEmpty()) || (resp9.isEmpty()) || (resp10.isEmpty())){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Erro")
                    .setMessage("Todos os Campos Devem Ser Preenchidos")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            // Eviar os valores para a próxima tela e mudar de tela
            Intent mudarTela = new Intent(getApplicationContext(), Pagina3.class);
            mudarTela.putExtra("objeto", respostas);
            startActivity(mudarTela);
        }
    }

    public void anterior(View view){
        Intent voltarTela = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(voltarTela); // Adicionado startActivity para efetivar a mudança de tela
    }
}