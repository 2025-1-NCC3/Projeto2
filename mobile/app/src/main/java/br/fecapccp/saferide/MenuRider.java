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

        checkCar = findViewById(R.id.carButton);
        tripButton = findViewById(R.id.rideButton);
        profileButton = findViewById(R.id.profileButton);
        formButton = findViewById(R.id.formButton);
        logoutButton = findViewById(R.id.logoutButton);

        checkCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuRider.this, CadastrarCarro.class);
                startActivity(intent);
            }
        });

        tripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuRider.this, ConfirmarViagemMotorista.class);
                startActivity(intent);
            }
        });

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuRider.this, editProfile.class);
                intent.putExtra("usuario", usuarioLogado);
                startActivity(intent);
            }
        });

        formButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MenuRider.this, PaginaPrincipal.class);
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(v -> {
            usuarioLogado = null;

            Intent intent = new Intent(MenuRider.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
