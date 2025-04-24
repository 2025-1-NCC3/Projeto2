package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import br.fecapccp.saferide.security.CryptoUtils;

public class Email extends AppCompatActivity {

    private EditText editEmail;
    private Button buttonNext, buttonBack;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email);

        // Recupera o objeto Usuario passado pela atividade Name
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Inicializa os componentes
        editEmail = findViewById(R.id.editEmail);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // Desabilita o botão "Próximo" inicialmente
        buttonNext.setEnabled(false);

        // Adiciona o TextWatcher ao campo de e-mail, com a mesma lógica presente em Name
        editEmail.addTextChangedListener(textWatcher);

        // Configura o botão "Próximo"
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();

                // Criptografa o objeto Email
                String emailCriptografado = CryptoUtils.encrypt(email);

                // Envia para o servidor
                enviarParaServidor(emailCriptografado);

                usuario.setEmail(email);

                // Passa o objeto Usuario para a próxima atividade (Number)
                Intent intent = new Intent(Email.this, Number.class);
                intent.putExtra("usuario", usuario);
                startActivity(intent);
            }
        });

        // Configura o botão "Voltar"
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volta para a tela de Name
                Intent intent = new Intent(Email.this, Genero.class);
                startActivity(intent);
                finish(); // Finaliza a atividade atual para liberar memória
            }
        });
    }

    // TextWatcher para validar o campo de e-mail
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Habilita o botão "Próximo" se o campo de e-mail for válido
            String email = editEmail.getText().toString().trim();
            buttonNext.setEnabled(isValidEmail(email));
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    // Método para validar o formato do e-mail importando Patterns do Android
    private boolean isValidEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void enviarParaServidor(String dadosCriptografados) {
        // Nossa URL para as Requests
        String url = "";

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
                // Envia o JSON criptografado
                return dadosCriptografados.getBytes();
            }

            @Override
            public String getBodyContentType() {
                // ou "application/json" se o backend esperar JSON
                return "text/plain";
            }
        };

        queue.add(stringRequest);
    }
}