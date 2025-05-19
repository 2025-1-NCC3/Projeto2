package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class editProfile extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private EditText userName, userEmail, userNumber, userPassword;
    private Button editProfileButton, deleteProfileButton;
    private ImageView backButton;
    private ProgressBar progressBarEditProfile;
    private Usuario usuario;
    private boolean isEditing = false;
    private String originalPasswordPlaceholder = "••••••••";
    private String originalName, originalEmail, originalNumber;


    private RequestQueue requestQueue;
    private final String BASE_URL = "https://98wyf8-3000.csb.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestQueue = Volley.newRequestQueue(this);

        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userNumber = findViewById(R.id.userNumber);
        userPassword = findViewById(R.id.userPassword);
        editProfileButton = findViewById(R.id.editProfile);
        deleteProfileButton = findViewById(R.id.deleteProfile);
        backButton = findViewById(R.id.backButton);


        userPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

        if (getIntent().hasExtra("usuario")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario");
        }
        if (usuario == null) {
            SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
            String usuarioJson = prefs.getString(Login.USER_DETAILS_KEY, null);
            if (usuarioJson != null) {
                usuario = new Gson().fromJson(usuarioJson, Usuario.class);
            }
        }

        if (usuario != null) {
            originalName = usuario.getName() != null ? usuario.getName() : "";
            originalEmail = usuario.getEmail() != null ? usuario.getEmail() : "";
            originalNumber = usuario.getPhone() != null ? usuario.getPhone() : (usuario.getNumber() != null ? usuario.getNumber() : "");

            userName.setText(originalName);
            userEmail.setText(originalEmail);
            userNumber.setText(originalNumber);
            userPassword.setText(originalPasswordPlaceholder);
            editProfileButton.setEnabled(true);
        } else {
            Toast.makeText(this, "Erro ao carregar dados do usuário.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setFieldsEditable(false);

        editProfileButton.setOnClickListener(v -> {
            isEditing = !isEditing;
            setFieldsEditable(isEditing);
            editProfileButton.setText(isEditing ? "Salvar Alterações" : "Editar Conta");

            if (!isEditing) {
                atualizarPerfilNoServidor();
            }
        });

        deleteProfileButton.setOnClickListener(v -> {
            Toast.makeText(editProfile.this, "Funcionalidade de deletar perfil ainda não implementada.", Toast.LENGTH_SHORT).show();
        });

        backButton.setOnClickListener(v -> {
            navigateBackToMenu();
        });
    }

    private void setFieldsEditable(boolean editable) {
        userName.setEnabled(false); // Nome nunca é editável
        userEmail.setEnabled(editable);
        userNumber.setEnabled(editable);
        userPassword.setEnabled(editable);

        if (editable) {
            userPassword.setText("");
            userPassword.setHint("Nova senha (deixe em branco para não alterar)");
        } else {
            if (usuario != null) { // Repopula com os dados originais ou placeholder
                userName.setText(originalName);
                userEmail.setText(originalEmail);
                userNumber.setText(originalNumber);
                userPassword.setText(originalPasswordPlaceholder);
            }
        }
    }

    private void atualizarPerfilNoServidor() {
        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados do usuário não disponíveis.", Toast.LENGTH_SHORT).show();
            return;
        }

        String novoEmail = userEmail.getText().toString().trim();
        String novoNumero = userNumber.getText().toString().trim();
        String novaSenha = userPassword.getText().toString().trim();

        if (novoEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(novoEmail).matches()) {
            userEmail.setError("Email inválido.");
            userEmail.requestFocus();
            revertToEditingStateOnError();
            return;
        }
        if (novoNumero.isEmpty()) {
            userNumber.setError("Número de telefone é obrigatório.");
            userNumber.requestFocus();
            revertToEditingStateOnError();
            return;
        }

        if (!novaSenha.isEmpty() && novaSenha.length() < 8) {
            userPassword.setError("A nova senha deve ter pelo menos 8 caracteres.");
            userPassword.requestFocus();
            revertToEditingStateOnError();
            return;
        }

        String urlUpdateUser = BASE_URL + "/user";
        if (progressBarEditProfile != null) progressBarEditProfile.setVisibility(View.VISIBLE);
        editProfileButton.setEnabled(false);
        deleteProfileButton.setEnabled(false);
        backButton.setEnabled(false);

        JSONObject jsonBody = new JSONObject();
        try {
            // Não envia 'name' nem 'surname'
            jsonBody.put("email", novoEmail);
            jsonBody.put("phone", novoNumero);

            if (!novaSenha.isEmpty()) {
                jsonBody.put("password", novaSenha);
            }

            // Campos como tipoConta, cpf, cnh, genero, birthday não são alterados aqui
            // Se precisar, eles devem ser adicionados ao JSON e o backend deve suportar
            // jsonBody.put("tipoConta", usuario.getTipoConta());
            // etc...

        } catch (JSONException e) {
            Log.e(TAG, "JSONException ao criar corpo da atualização: " + e.getMessage());
            if (progressBarEditProfile != null) progressBarEditProfile.setVisibility(View.GONE);
            editProfileButton.setEnabled(true); deleteProfileButton.setEnabled(true); backButton.setEnabled(true);
            Toast.makeText(this, "Erro ao preparar dados para atualização.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Enviando para /user (PUT): " + jsonBody.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, urlUpdateUser, jsonBody,
                response -> {
                    if (progressBarEditProfile != null) progressBarEditProfile.setVisibility(View.GONE);
                    editProfileButton.setEnabled(true); deleteProfileButton.setEnabled(true); backButton.setEnabled(true);
                    Log.d(TAG, "Resposta updateUser: " + response.toString());
                    try {
                        JSONObject updatedUserJson = response.getJSONObject("user");

                        usuario.setName(updatedUserJson.optString("name", usuario.getName())); // Mantém o nome original se não vier
                        usuario.setEmail(updatedUserJson.optString("email", novoEmail));
                        usuario.setPhone(updatedUserJson.optString("phone", novoNumero));

                        originalName = usuario.getName(); // Atualiza os "originais" para o próximo ciclo de edição
                        originalEmail = usuario.getEmail();
                        originalNumber = usuario.getPhone();

                        if (!novaSenha.isEmpty()) {
                            userPassword.setText(originalPasswordPlaceholder);
                        }

                        SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(Login.USER_DETAILS_KEY, usuario.toJson());
                        editor.apply();

                        Toast.makeText(editProfile.this, "Perfil atualizado com sucesso!", Toast.LENGTH_LONG).show();
                        isEditing = false;
                        setFieldsEditable(false);
                        editProfileButton.setText("Editar Conta");

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException ao parsear resposta de updateUser: " + e.getMessage());
                        Toast.makeText(editProfile.this, "Erro ao processar resposta do servidor.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (progressBarEditProfile != null) progressBarEditProfile.setVisibility(View.GONE);
                    editProfileButton.setEnabled(true); deleteProfileButton.setEnabled(true); backButton.setEnabled(true);

                    String mensagemErro = "Falha ao atualizar perfil.";
                    String responseBody = "";
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Erro Volley updateUser - Status: " + error.networkResponse.statusCode);
                        if (error.networkResponse.data != null) {
                            try {
                                responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject data = new JSONObject(responseBody);
                                if (data.has("error")) mensagemErro = data.getString("error");
                                else if (data.has("details")) mensagemErro = data.getString("details");
                                else if (data.has("message")) mensagemErro = data.getString("message");
                                Log.e(TAG, "Corpo da resposta do erro (updateUser): " + responseBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Erro parse erro Volley (updateUser): " + e.getMessage() + "; Corpo Original: " + responseBody);
                                if(!responseBody.isEmpty() && !responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                                    mensagemErro = responseBody;
                                }
                            }
                        }
                    } else if (error.getMessage() != null) {
                        Log.e(TAG, "Erro Volley (updateUser): " + error.getMessage());
                        mensagemErro = "Erro de conexão ou servidor indisponível.";
                    }
                    Toast.makeText(editProfile.this, mensagemErro, Toast.LENGTH_LONG).show();
                    revertToEditingStateOnError();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
                String token = prefs.getString(Login.USER_TOKEN_KEY, null);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                } else {
                    Log.w(TAG, "Token JWT não encontrado para atualizar perfil.");
                    Toast.makeText(editProfile.this, "Sessão inválida. Faça login novamente.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(editProfile.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    private void revertToEditingStateOnError() {
        setFieldsEditable(true);
        editProfileButton.setText("Salvar Alterações");
        isEditing = true;
    }


    private void navigateBackToMenu() {
        if (usuario == null) {
            Intent intent = new Intent(editProfile.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent;
        String tipoConta = usuario.getTipoConta();
        String genero = usuario.getGenero();

        if ("Prefiro não identificar".equalsIgnoreCase(genero)) {
            intent = new Intent(editProfile.this, Menu.class);
        } else if ("Motorista".equalsIgnoreCase(tipoConta)) {
            intent = new Intent(editProfile.this, MenuRider.class);
        } else {
            intent = new Intent(editProfile.this, Menu.class);
        }
        intent.putExtra("usuario", usuario);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isEditing) {
            isEditing = false;
            setFieldsEditable(false);
            editProfileButton.setText("Editar Conta");
        } else {
            navigateBackToMenu();
        }
    }
}