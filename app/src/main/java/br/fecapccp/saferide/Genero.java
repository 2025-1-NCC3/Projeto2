package br.fecapccp.saferide;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class Genero extends AppCompatActivity {

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
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");
        spinnerGender = findViewById(R.id.spinnerGender);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        String[] generos = {"Masculino", "Feminino", "Prefiro não identificar"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                generos
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        // Listener para selecionar o gênero
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = generos[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGender = "Prefiro não identificar"; // valor padrão
            }
        });

    }
}