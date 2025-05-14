package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import br.fecap.pi.saferide.security.CryptoUtils;
import br.fecap.pi.saferide.R;

public class Number extends AppCompatActivity {

    private Spinner spinnerDDI;
    private EditText editPhone;
    private Button buttonNext, buttonBack;
    private String selectedDDI;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.number);

        // Ajustando padding para evitar sobreposição com barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade Email
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Ligando os elementos do XML às variáveis do Java
        spinnerDDI = findViewById(R.id.spinnerDDI);
        editPhone = findViewById(R.id.editPhone);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // Mapa com DDIs dos países
        final HashMap<String, String> countryCodes = new HashMap<>();
        countryCodes.put("(+55)", "55");
        countryCodes.put("(+1)", "1");
        countryCodes.put("(+54)", "54");
        countryCodes.put("(+351)", "351");
        countryCodes.put("(+44)", "44");

        // Criando um array com os nomes dos países
        String[] countryArray = countryCodes.keySet().toArray(new String[0]);

        // Criando um Adapter para o Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, countryArray);
        spinnerDDI.setAdapter(adapter);

        // Listener para capturar o DDI selecionado
        spinnerDDI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String country = (String) parent.getItemAtPosition(position);
                selectedDDI = countryCodes.get(country);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDDI = "55"; // Padrão: Brasil
            }
        });

        // Listener para o botão "Próximo"
        buttonNext.setOnClickListener(v -> {
            String phone = editPhone.getText().toString().trim();
            String fullPhoneNumber = "+" + selectedDDI + phone;

            if (phone.isEmpty()) {
                Toast.makeText(Number.this, "Digite o número de telefone!", Toast.LENGTH_SHORT).show();
            } else if (!PhoneNumberUtils.isGlobalPhoneNumber(fullPhoneNumber)) {
                Toast.makeText(Number.this, "Número de telefone inválido!", Toast.LENGTH_SHORT).show();
            } else {
                // Atualiza o objeto Usuario com o número de telefone
                usuario.setNumber(fullPhoneNumber);

                // Criptografa o objeto Number
                String numeroCriptografado = CryptoUtils.encrypt(phone);

                // Monta o JSON
                JSONObject json = new JSONObject();
                try{
                    json.put("numero", numeroCriptografado);
                }  catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Envia para o servidor
                enviarParaServidor(json.toString());

                // Passa o objeto Usuario para a próxima atividade (Password)
                Intent intent = new Intent(Number.this, Password.class);
                intent.putExtra("usuario", usuario);
                startActivity(intent);
            }
        });

        // Listener para o botão "Voltar"
        buttonBack.setOnClickListener(v -> {
            // Volta para a tela de Email
            Intent intent = new Intent(Number.this, Email.class);
            startActivity(intent);
            finish(); // Finaliza a atividade atual para liberar memória
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