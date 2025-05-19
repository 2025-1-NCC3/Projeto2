package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
// import android.util.Log; // Não estritamente necessário após as modificações
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

// Imports de Volley, JSON e CryptoUtils foram removidos pois não são usados nesta activity

public class Genero extends AppCompatActivity {

    private static final String TAG = "GeneroActivity";
    private Spinner spinnerGender;
    private MaterialButton buttonNext, buttonBack;
    private String selectedGender;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_genero);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent().hasExtra("usuario_parcial")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario_parcial");
        }

        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados de cadastro incompletos. Reiniciando.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Name.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        spinnerGender = findViewById(R.id.spinnerGender);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // VOLTANDO PARA A DEFINIÇÃO ORIGINAL DO ARRAY DE GÊNEROS DIRETAMENTE NO CÓDIGO
        final String[] generos = {"Selecione seu gênero", "Masculino", "Feminino", "Prefiro não identificar"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                generos
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        buttonNext.setEnabled(false);

        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedGender = null;
                    buttonNext.setEnabled(false);
                } else {
                    selectedGender = generos[position];
                    buttonNext.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGender = null;
                buttonNext.setEnabled(false);
            }
        });

        buttonNext.setOnClickListener(v -> {
            if (selectedGender == null || selectedGender.equals(generos[0])) {
                Toast.makeText(Genero.this, "Por favor, selecione um gênero válido.", Toast.LENGTH_SHORT).show();
                return;
            }

            usuario.setGenero(selectedGender);

            Intent intent = new Intent(Genero.this, Email.class); // Próxima Activity
            intent.putExtra("usuario_parcial", usuario);
            startActivity(intent);
        });

        buttonBack.setOnClickListener(v -> {
            finish();
        });
    }
}