package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import br.fecap.pi.saferide.R;
import br.fecap.pi.saferide.security.CryptoUtils;

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

        // Botão Próximo
        buttonNext.setOnClickListener(v -> {
            usuario.setGenero(selectedGender); // salva o gênero no objeto Usuario

            // Criptografa o objeto Gênero
            String generoCriptografado = CryptoUtils.encrypt(selectedGender);

            // Monta o JSON
            JSONObject json = new JSONObject();
            try{
                json.put("id", getId());
                json.put("genero", generoCriptografado);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            // Envia para o servidor
            enviarParaServidor(json.toString());

            // Redireciona para a próxima tela, passando o objeto atualizado
            Intent intent = new Intent(Genero.this, Email.class); // Troque "ProximaTela.class" pela sua Activity real
            intent.putExtra("usuario", usuario);
            startActivity(intent);
        });

        // Botão Voltar
        buttonBack.setOnClickListener(v -> {
            finish(); // Finaliza a activity atual e volta para anterior
        });
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
