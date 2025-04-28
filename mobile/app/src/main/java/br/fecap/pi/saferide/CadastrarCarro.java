package br.fecap.pi.saferide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import br.fecap.pi.saferide.R;

public class CadastrarCarro extends AppCompatActivity {

    private EditText editBrand, editModel, editPlaca;
    private Spinner spinnerColor, spinnerAno;
    private ImageView backButton;
    private Button btnCadastrar;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cadastrar_carro);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o usuário vindo da tela anterior
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Inicializa os componentes da tela
        editBrand = findViewById(R.id.editBrand);
        editModel = findViewById(R.id.editModel);
        editPlaca = findViewById(R.id.editPlaca);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerAno = findViewById(R.id.spinnerAno);
        btnCadastrar = findViewById(R.id.button2);
        backButton = findViewById(R.id.backButton); // <- Aqui está a linha adicionada

        btnCadastrar.setOnClickListener(v -> cadastrarCarro());

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CadastrarCarro.this, MenuRider.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
            finish();
        });
    }

    private void cadastrarCarro() {
        String marca = editBrand.getText().toString().trim();
        String modelo = editModel.getText().toString().trim();
        String placa = editPlaca.getText().toString().trim();
        String cor = spinnerColor.getSelectedItem().toString();
        String ano = spinnerAno.getSelectedItem().toString();

        if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Carro carro = new Carro(marca, modelo, cor, ano, placa);

        // Associa o carro ao usuário
        usuario.setCarro(carro);

        // Salva o usuário com o carro no SharedPreferences
        SharedPreferences preferences = getSharedPreferences("usuario_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String usuarioJson = gson.toJson(usuario);
        editor.putString("usuario_logado", usuarioJson);
        editor.apply();

        Toast.makeText(this, "Carro cadastrado com sucesso!", Toast.LENGTH_SHORT).show();

        // Volta para a tela principal
        Intent intent = new Intent(CadastrarCarro.this, MenuRider.class);
        intent.putExtra("usuario", usuario);
        startActivity(intent);
        finish();
    }
}
