package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Password extends AppCompatActivity {

    private static final String TAG = "PasswordActivity";
    private EditText editPassword;
    private Button buttonNext, buttonBack;
    private ProgressBar progressBarPassword;
    private Usuario usuario;

    private RequestQueue requestQueue;
    private final String BASE_URL = "https://98wyf8-3000.csb.app";

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

        requestQueue = Volley.newRequestQueue(this);

        if (getIntent().hasExtra("usuario_parcial")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario_parcial");
        }

        if (usuario == null) {
            Toast.makeText(this, "Erro crítico: Dados de cadastro não encontrados. Reiniciando.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Name.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        editPassword = findViewById(R.id.editPassword);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);
        progressBarPassword = findViewById(R.id.password_progress_bar);

        buttonNext.setEnabled(false);
        editPassword.addTextChangedListener(textWatcher);

        buttonNext.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();

            if (password.isEmpty()) {
                editPassword.setError("Senha é obrigatória.");
                editPassword.requestFocus();
                return;
            }
            if (password.length() < 8) {
                editPassword.setError("A senha deve ter pelo menos 8 caracteres.");
                editPassword.requestFocus();
                return;
            }
            usuario.setPassword(password);
            realizarCadastroNoServidor();
        });

        buttonBack.setOnClickListener(v -> {
            finish();
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String password = editPassword.getText().toString().trim();
            buttonNext.setEnabled(!password.isEmpty() && password.length() >= 8);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    private void realizarCadastroNoServidor() {
        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados do usuário incompletos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String urlSignup = BASE_URL + "/auth/signup";
        if (progressBarPassword != null) progressBarPassword.setVisibility(View.VISIBLE);
        buttonNext.setEnabled(false);
        buttonBack.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", usuario.getName());
            // O campo "surname" não será enviado para evitar o erro "no such column" no backend
            jsonBody.put("email", usuario.getEmail());
            jsonBody.put("phone", usuario.getPhone() != null ? usuario.getPhone() : usuario.getNumber());

            if (usuario.getBirthday() == null || usuario.getBirthday().isEmpty() || "1900-01-01".equals(usuario.getBirthday())) {
                // Lembre-se de implementar a coleta REAL da data de nascimento
                // e garantir que ela chegue aqui no formato YYYY-MM-DD
                // Se não for coletada, o backend (allowNull: false) pode rejeitar.
                // Para fins de teste, se ainda não coletou, pode enviar um placeholder válido se o backend aceitar,
                // mas isso não é o ideal para produção.
                Log.e(TAG, "Data de Nascimento não preenchida ou inválida no objeto Usuario.");
                Toast.makeText(this, "Data de nascimento não foi fornecida.", Toast.LENGTH_LONG).show();
                // Para o teste, vamos enviar um placeholder se estiver nulo, mas isso DEVE ser corrigido
                jsonBody.put("birthday", "1990-01-01"); // PLACEHOLDER - CORRIGIR COLETA
            } else {
                jsonBody.put("birthday", usuario.getBirthday());
            }

            jsonBody.put("genero", usuario.getGenero());
            jsonBody.put("tipoConta", usuario.getTipoConta());
            jsonBody.put("password", usuario.getPassword());

            if ("Motorista".equals(usuario.getTipoConta())) {
                if (usuario.getCnh() != null && !usuario.getCnh().isEmpty()) {
                    jsonBody.put("cnh", usuario.getCnh());
                } else {
                    Toast.makeText(this, "CNH é obrigatória para motorista.", Toast.LENGTH_LONG).show();
                    if (progressBarPassword != null) progressBarPassword.setVisibility(View.GONE);
                    buttonNext.setEnabled(true); buttonBack.setEnabled(true);
                    return;
                }
                // Se CPF for opcional ou não aplicável para motorista
                if (usuario.getCpf() != null && !usuario.getCpf().isEmpty()) {
                    // jsonBody.put("cpf", usuario.getCpf()); // Descomente se o backend espera/aceita CPF para motorista
                }
            } else if ("Passageiro".equals(usuario.getTipoConta())) {
                if (usuario.getCpf() != null && !usuario.getCpf().isEmpty()) {
                    jsonBody.put("cpf", usuario.getCpf());
                } else {
                    Toast.makeText(this, "CPF é obrigatório para passageiro.", Toast.LENGTH_LONG).show();
                    if (progressBarPassword != null) progressBarPassword.setVisibility(View.GONE);
                    buttonNext.setEnabled(true); buttonBack.setEnabled(true);
                    return;
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSONException ao criar corpo do cadastro: " + e.getMessage());
            if (progressBarPassword != null) progressBarPassword.setVisibility(View.GONE);
            buttonNext.setEnabled(true); buttonBack.setEnabled(true);
            Toast.makeText(this, "Erro ao preparar dados para cadastro.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Enviando para /auth/signup: " + jsonBody.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, urlSignup, jsonBody,
                response -> {
                    if (progressBarPassword != null) progressBarPassword.setVisibility(View.GONE);
                    buttonNext.setEnabled(true); buttonBack.setEnabled(true);
                    Log.d(TAG, "Resposta Signup: " + response.toString());
                    try {
                        JSONObject userJsonFromServer = response.getJSONObject("user");
                        String token = response.getString("token");

                        usuario.setId(userJsonFromServer.optInt("id"));
                        usuario.setName(userJsonFromServer.optString("name"));
                        usuario.setEmail(userJsonFromServer.optString("email"));
                        // Adicione outros campos do usuário que o servidor retorna e você queira atualizar no objeto local

                        SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Login.USER_TOKEN_KEY, token);
                        editor.putString(Login.USER_DETAILS_KEY, usuario.toJson());
                        editor.apply();

                        Toast.makeText(Password.this, "Cadastro realizado com sucesso!", Toast.LENGTH_LONG).show();

                        Intent intent;
                        String tipoConta = usuario.getTipoConta();
                        String genero = usuario.getGenero();

                        if ("Prefiro não identificar".equalsIgnoreCase(genero)) {
                            intent = new Intent(Password.this, Menu.class);
                        } else if ("Motorista".equals(tipoConta)) {
                            intent = new Intent(Password.this, MenuRider.class);
                        } else {
                            intent = new Intent(Password.this, Menu.class);
                        }

                        intent.putExtra("usuario", usuario);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException ao parsear resposta de cadastro: " + e.getMessage());
                        Toast.makeText(Password.this, "Erro ao processar resposta do servidor.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBarPassword != null) progressBarPassword.setVisibility(View.GONE);
                    buttonNext.setEnabled(true); buttonBack.setEnabled(true);
                    String mensagemErro = "Falha no cadastro. Tente novamente.";
                    String responseBody = "";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            if (data.has("error")) {
                                mensagemErro = data.getString("error");
                            } else if (data.has("details")){
                                mensagemErro = data.getString("details");
                            } else if (data.has("errors") && data.get("errors") instanceof JSONArray) {
                                JSONArray errorsArray = data.getJSONArray("errors");
                                if (errorsArray.length() > 0) {
                                    Object firstError = errorsArray.get(0);
                                    if (firstError instanceof JSONObject) {
                                        mensagemErro = ((JSONObject) firstError).optString("message", mensagemErro);
                                    } else if (firstError instanceof String) {
                                        mensagemErro = (String) firstError;
                                    }
                                }
                            }
                            Log.e(TAG, "Corpo da resposta do erro (status " + error.networkResponse.statusCode + "): " + responseBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao parsear corpo do erro Volley (cadastro): " + e.getMessage() + "; Corpo original: " + responseBody);
                            if (!responseBody.isEmpty() && !responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                                mensagemErro = responseBody;
                            }
                        }
                    } else if (error.getMessage() != null) {
                        Log.e(TAG, "Erro Volley (cadastro, sem networkResponse): " + error.getMessage());
                        mensagemErro = "Erro de conexão ou no servidor. Verifique sua internet.";
                    }
                    Log.e(TAG, "Erro Volley no cadastro (final): " + mensagemErro, error);
                    Toast.makeText(Password.this, mensagemErro, Toast.LENGTH_LONG).show();
                });

        requestQueue.add(jsonObjectRequest);
    }
}