package br.fecap.pi.saferide;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import br.fecap.pi.saferide.R;
import br.fecap.pi.saferide.security.CryptoUtils;

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

        // Criptografa para as informações do carro
        String encriptedMarca = CryptoUtils.encrypt(marca);
        String encriptedModelo = CryptoUtils.encrypt(modelo);
        String encriptedPlaca = CryptoUtils.encrypt(placa);
        String encriptedCor = CryptoUtils.encrypt(cor);
        String encriptedAno = CryptoUtils.encrypt(ano);

        // Prepara o JSON
        JSONObject json = new JSONObject();
        try {
            json.put("id", usuario.getId()); // Para relacionar as informações a um usuário
            json.put("marca", encriptedMarca);
            json.put("modelo", encriptedModelo);
            json.put("placa", encriptedPlaca);
            json.put("cor", encriptedCor);
            json.put("ano", encriptedAno);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        // Envia para o servidor
        enviarParaServidor(json.toString());

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

    private void enviarParaServidor(String dadosCriptografados) {
        String url = ""; // URL da API

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("API", "Resposta: " + response);
                },
                error -> {
                    Log.e("API", "Erro: ", error);
                }
        ) {
            @Override
            public byte[] getBody() {
                return dadosCriptografados.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json"; // Envio do JSON
            }
        };

        queue.add(stringRequest);
    }
}
