package br.fecap.pi.saferide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;

public class CadastrarCarro extends AppCompatActivity {

    private static final String TAG = "CadastrarCarroActivity";
    private EditText editBrand, editModel, editPlaca;
    private Spinner spinnerColor, spinnerAno;
    private ImageView backButton;
    private MaterialButton btnCadastrar;
    private Usuario usuarioLogado;

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

        if (getIntent().hasExtra("usuario")) {
            usuarioLogado = (Usuario) getIntent().getSerializableExtra("usuario");
        }

        if (usuarioLogado == null) {
            Toast.makeText(this, "Erro: Usuário não encontrado. Tente novamente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        editBrand = findViewById(R.id.editBrand);
        editModel = findViewById(R.id.editModel);
        editPlaca = findViewById(R.id.editPlaca);
        spinnerColor = findViewById(R.id.spinnerColor);
        spinnerAno = findViewById(R.id.spinnerAno);
        btnCadastrar = findViewById(R.id.button2);
        backButton = findViewById(R.id.backButton);

        setupAnoSpinner();
        setupCorSpinner();

        if (usuarioLogado.getCarro() != null) {
            preencherDadosCarro(usuarioLogado.getCarro());
        }

        btnCadastrar.setOnClickListener(v -> salvarCarroLocalmente());

        backButton.setOnClickListener(v -> finish());
    }

    private void preencherDadosCarro(Carro carro) {
        editBrand.setText(carro.getMarca());
        editModel.setText(carro.getModelo());
        editPlaca.setText(carro.getPlaca());

        if (spinnerColor.getAdapter() != null) {
            for (int i = 0; i < spinnerColor.getAdapter().getCount(); i++) {
                if (spinnerColor.getAdapter().getItem(i).toString().equalsIgnoreCase(carro.getCor())) {
                    spinnerColor.setSelection(i);
                    break;
                }
            }
        }
        if (spinnerAno.getAdapter() != null) {
            for (int i = 0; i < spinnerAno.getAdapter().getCount(); i++) {
                if (spinnerAno.getAdapter().getItem(i).toString().equals(carro.getAno())) {
                    spinnerAno.setSelection(i);
                    break;
                }
            }
        }
    }


    private void setupAnoSpinner() {
        ArrayList<String> anos = new ArrayList<>();
        int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = anoAtual + 1; i >= anoAtual - 25; i--) {
            anos.add(Integer.toString(i));
        }
        ArrayAdapter<String> anoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, anos);
        anoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAno.setAdapter(anoAdapter);
    }

    private void setupCorSpinner() {
        ArrayAdapter<CharSequence> corAdapter = ArrayAdapter.createFromResource(this,
                R.array.car_colors_array, android.R.layout.simple_spinner_item);
        corAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColor.setAdapter(corAdapter);
    }

    private boolean validarCampos(String marca, String modelo, String placa, String cor, String ano) {
        if (marca.isEmpty()) {
            editBrand.setError("Marca é obrigatória");
            editBrand.requestFocus();
            return false;
        }
        if (modelo.isEmpty()) {
            editModel.setError("Modelo é obrigatório");
            editModel.requestFocus();
            return false;
        }
        if (placa.isEmpty()) {
            editPlaca.setError("Placa é obrigatória");
            editPlaca.requestFocus();
            return false;
        }
        if (!placa.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}") && !placa.matches("[A-Z]{3}[0-9]{4}")) {
            editPlaca.setError("Formato de placa inválido");
            editPlaca.requestFocus();
            return false;
        }
        if (cor == null || cor.isEmpty() || cor.equals(getResources().getStringArray(R.array.car_colors_array)[0])) {
            Toast.makeText(this, "Selecione uma cor válida.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (ano == null || ano.isEmpty()) {
            Toast.makeText(this, "Selecione um ano válido.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void salvarCarroLocalmente() {
        String marca = editBrand.getText().toString().trim();
        String modelo = editModel.getText().toString().trim();
        String placa = editPlaca.getText().toString().trim().toUpperCase();
        String cor = spinnerColor.getSelectedItem().toString();
        String ano = spinnerAno.getSelectedItem().toString();

        if (!validarCampos(marca, modelo, placa, cor, ano)) {
            return;
        }

        Carro carro = new Carro(marca, modelo, cor, ano, placa);
        usuarioLogado.setCarro(carro);

        SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Login.USER_DETAILS_KEY, usuarioLogado.toJson());
        editor.apply();

        Toast.makeText(this, "Dados do carro salvos localmente!", Toast.LENGTH_LONG).show();
        finish();
    }
}