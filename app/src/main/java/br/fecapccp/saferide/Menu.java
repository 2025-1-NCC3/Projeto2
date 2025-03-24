package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Menu extends AppCompatActivity {

    private Button rideButton, historyButton, profileButton, formButton, logoutButton;
    private Usuario usuarioLogado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.menu);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade de login
        usuarioLogado = (Usuario) getIntent().getSerializableExtra("usuario");

        rideButton = findViewById(R.id.rideButton);
        historyButton = findViewById(R.id.historyButton);
        profileButton = findViewById(R.id.profileButton);
        formButton = findViewById(R.id.formButton);

        // Adicione o botão de logout no seu layout XML
        logoutButton = findViewById(R.id.logoutButton);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, editProfile.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            }
        });

        formButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Menu.this, PaginaPrincipal.class);
                startActivity(intent);
            }
        });

        // Implementação do botão de logout
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Limpa o usuário logado
                usuarioLogado = null;

                // Redireciona para a tela de login
                Intent intent = new Intent(Menu.this, Login.class);

                // Limpa a pilha de activities para que não seja possível voltar
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish(); // Finaliza a activity atual
            }
        });
    }
}