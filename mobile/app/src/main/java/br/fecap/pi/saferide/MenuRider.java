package br.fecap.pi.saferide;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

public class MenuRider extends AppCompatActivity {
    private Button checkCar, tripButton, profileButton, formButton, logoutButton;
    private Usuario usuarioLogado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_rider);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        usuarioLogado = (Usuario) getIntent().getSerializableExtra("usuario");

        if (usuarioLogado == null) {
            SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
            String usuarioJson = prefs.getString(Login.USER_DETAILS_KEY, null);
            if (usuarioJson != null) {
                Gson gson = new Gson();
                usuarioLogado = gson.fromJson(usuarioJson, Usuario.class);
            } else {
                Toast.makeText(this, "Sessão não encontrada. Por favor, faça login novamente.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MenuRider.this, Login.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }

        checkCar = findViewById(R.id.carButton);
        tripButton = findViewById(R.id.rideButton);
        profileButton = findViewById(R.id.profileButton);
        formButton = findViewById(R.id.formButton);
        logoutButton = findViewById(R.id.logoutButton);

        checkCar.setOnClickListener(v -> {
            if (usuarioLogado != null) {
                Intent intent = new Intent(MenuRider.this, CadastrarCarro.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            } else {
                handleUserNotAvailable();
            }
        });

        tripButton.setOnClickListener(v -> {
            if (usuarioLogado != null) {
                Intent intent = new Intent(MenuRider.this, ConfirmarViagemMotorista.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            } else {
                handleUserNotAvailable();
            }
        });

        profileButton.setOnClickListener(v -> {
            if (usuarioLogado != null) {
                Intent intent = new Intent(MenuRider.this, editProfile.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            } else {
                handleUserNotAvailable();
            }
        });

        formButton.setOnClickListener(v -> {
            if (usuarioLogado != null) {
                Intent intent = new Intent(MenuRider.this, PaginaPrincipal.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            } else {
                handleUserNotAvailable();
            }
        });

        logoutButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(Login.USER_TOKEN_KEY);
            editor.remove(Login.USER_DETAILS_KEY);
            editor.apply();

            usuarioLogado = null;

            Intent intent = new Intent(MenuRider.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (usuarioLogado == null) {
            SharedPreferences prefs = getSharedPreferences(Login.AUTH_PREFS, MODE_PRIVATE);
            String usuarioJson = prefs.getString(Login.USER_DETAILS_KEY, null);
            if (usuarioJson != null) {
                Gson gson = new Gson();
                usuarioLogado = gson.fromJson(usuarioJson, Usuario.class);
            } else {
                // Se ainda for nulo após tentar carregar das SharedPreferences,
                // significa que não há sessão válida.
                if (!isFinishing()) { // Evita mostrar Toast se a activity já está finalizando
                    handleUserNotAvailable();
                }
            }
        }
    }

    private void handleUserNotAvailable() {
        Toast.makeText(MenuRider.this, "Erro: Dados do usuário não disponíveis. Faça login novamente.", Toast.LENGTH_LONG).show();
        Intent loginIntent = new Intent(MenuRider.this, Login.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }
}