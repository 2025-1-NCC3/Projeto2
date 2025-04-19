package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PaginaPrincipal extends AppCompatActivity {

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
    }

    public void seguinte(View view) {
        CheckBox checkBox = findViewById(R.id.checkBoxPrivacy);

        if (!checkBox.isChecked()) {
            Toast.makeText(this, "Você deve aceitar a Política de Privacidade para continuar.", Toast.LENGTH_LONG).show();
        } else {
            Intent mudarTela = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(mudarTela);
        }
    }

    public void abrirPolitica(View view) {
        Intent intent = new Intent(this, PoliticaPrivacidade.class);
        startActivity(intent);
    }
}