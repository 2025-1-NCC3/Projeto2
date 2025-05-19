package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.fecap.pi.saferide.R;

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

            rideButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Menu.this, ConfirmarViagem.class);
                    startActivity(intent);
                }
            });

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


        }
    }