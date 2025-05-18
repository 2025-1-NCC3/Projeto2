package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONException;
import org.json.JSONObject;

import br.fecap.pi.saferide.R;

public class Password extends AppCompatActivity {

    private EditText editPassword;
    private Button buttonNext, buttonBack;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        editPassword = findViewById(R.id.editPassword);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        buttonNext.setEnabled(false);
        editPassword.addTextChangedListener(textWatcher);

        buttonNext.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(Password.this, "Digite a senha!", Toast.LENGTH_SHORT).show();
            } else {
                // Gera o salt e hash da senha
                String salt = PasswordUtils.generateSalt();
                String hashedPassword = PasswordUtils.hashPassword(password, salt);

                usuario.setSalt(salt);

                // Atualiza o objeto Usuario com a senha
                usuario.setPassword(hashedPassword);

                // Monta o JSON
                JSONObject json = new JSONObject();
                try{
                    json.put("id", getId()); // Usando o método getId() adicionado
                    json.put("senha", hashedPassword);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // Envia para o servidor
                enviarParaServidor(json.toString());

                // Simula cadastro no sistema
                Login.cadastrarUsuario(usuario);

                try {
                    Intent intent;
                    if ("Motorista".equals(usuario.getTipoConta())) {
                        intent = new Intent(Password.this, MenuRider.class);
                    } else {
                        intent = new Intent(Password.this, Menu.class);
                    }

                    intent.putExtra("usuario", usuario);
                    startActivity(intent);
                    finish(); // Encerra esta tela
                } catch (Exception e) {
                    Toast.makeText(this, "Erro ao abrir menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(Password.this, Number.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);
            finish();
        });
    }

    // Método para obter o ID do usuário (adicionado para resolver o problema)
    private int getId() {
        if (usuario != null) {
            return usuario.getId();
        }
        return -1; // Valor padrão caso o usuário não esteja disponível
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String password = editPassword.getText().toString().trim();
            buttonNext.setEnabled(!password.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

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