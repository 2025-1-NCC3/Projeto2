package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TextView text_tela_cadastro;
    private EditText editEmail, editSenha;
    private Button buttonLogin;
    private ProgressBar progressBarLogin;

    private RequestQueue requestQueue;
    private final String BASE_URL = "https://98wyf8-3000.csb.app";
    public static final String AUTH_PREFS = "AuthPrefs";
    public static final String USER_TOKEN_KEY = "UserToken";
    public static final String USER_DETAILS_KEY = "UserDetails";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestQueue = Volley.newRequestQueue(this);

        text_tela_cadastro = findViewById(R.id.text_cadastro);
        editEmail = findViewById(R.id.editTextText1);
        editSenha = findViewById(R.id.editTextText2);
        buttonLogin = findViewById(R.id.button2);
        progressBarLogin = findViewById(R.id.login_progress_bar);

        text_tela_cadastro.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Name.class);
            startActivity(intent);
        });

        buttonLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(Login.this, "Por favor, preencha email e senha.", Toast.LENGTH_SHORT).show();
                return;
            }
            tentarLoginServidor(email, senha);
        });
    }

    private void tentarLoginServidor(String email, String senha) {
        String urlSignin = BASE_URL + "/auth/signin";
        if (progressBarLogin != null) progressBarLogin.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", senha);
        } catch (JSONException e) {
            Log.e(TAG, "JSONException ao criar corpo do login: " + e.getMessage());
            if (progressBarLogin != null) progressBarLogin.setVisibility(View.GONE);
            buttonLogin.setEnabled(true);
            Toast.makeText(this, "Erro ao preparar dados de login.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Enviando para /auth/signin: " + jsonBody.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, urlSignin, jsonBody,
                response -> {
                    if (progressBarLogin != null) progressBarLogin.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);
                    Log.d(TAG, "Resposta Login: " + response.toString());
                    try {
                        JSONObject userJsonFromServer = response.getJSONObject("user");
                        String token = response.getString("token");

                        Gson gson = new Gson();
                        Usuario usuarioLogado = gson.fromJson(userJsonFromServer.toString(), Usuario.class);
                        // O ID já deve vir do servidor e ser parseado pelo Gson se estiver no JSON "user"
                        // usuarioLogado.setId(userJsonFromServer.optInt("id")); // Redundante se Gson mapeia 'id'

                        SharedPreferences prefs = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(USER_TOKEN_KEY, token);
                        editor.putString(USER_DETAILS_KEY, usuarioLogado.toJson());
                        editor.apply();

                        Toast.makeText(Login.this, "Login bem-sucedido! Bem-vindo, " + usuarioLogado.getName(), Toast.LENGTH_LONG).show();

                        Intent intent;
                        String tipoConta = usuarioLogado.getTipoConta();
                        String genero = usuarioLogado.getGenero(); // Pega o gênero do usuário logado

                        // Lógica de navegação corrigida
                        if ("Prefiro não identificar".equalsIgnoreCase(genero)) {
                            intent = new Intent(Login.this, Menu.class);
                        } else if ("Motorista".equalsIgnoreCase(tipoConta)) {
                            intent = new Intent(Login.this, MenuRider.class);
                        } else { // Passageiro (e não "Prefiro não identificar")
                            intent = new Intent(Login.this, Menu.class);
                        }

                        intent.putExtra("usuario", usuarioLogado);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finishAffinity();

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException ao parsear resposta de login: " + e.getMessage());
                        Toast.makeText(Login.this, "Erro ao processar resposta do servidor.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBarLogin != null) progressBarLogin.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);
                    String mensagemErro = "Falha no login. Verifique suas credenciais.";
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
                            Log.e(TAG, "Corpo da resposta do erro (login, status " + error.networkResponse.statusCode + "): " + responseBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Erro ao parsear corpo do erro Volley (login): " + e.getMessage() + "; Corpo original: " + responseBody);
                            if (!responseBody.isEmpty() && !responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                                mensagemErro = responseBody;
                            }
                        }
                    } else if (error.getMessage() != null) {
                        Log.e(TAG, "Erro Volley (login, sem networkResponse): " + error.getMessage());
                        mensagemErro = "Erro de conexão ou no servidor. Verifique sua internet.";
                    }
                    Log.e(TAG, "Erro Volley no login (final): " + mensagemErro, error);
                    Toast.makeText(Login.this, mensagemErro, Toast.LENGTH_LONG).show();
                });

        requestQueue.add(jsonObjectRequest);
    }
}