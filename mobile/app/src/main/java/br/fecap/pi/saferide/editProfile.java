package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.fecap.pi.saferide.R;

public class editProfile extends AppCompatActivity {

    private EditText userName, userEmail, userNumber, userPassword;
    private Button editProfileButton, deleteProfileButton;
    private ImageView backButton;
    private Usuario usuario;
    private boolean isEditing = false;

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

        // Inicializa os componentes
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userNumber = findViewById(R.id.userNumber);
        userPassword = findViewById(R.id.userPassword);
        editProfileButton = findViewById(R.id.editProfile);
        deleteProfileButton = findViewById(R.id.deleteProfile);
        backButton = findViewById(R.id.backButton);

        // Mostra a senha com bolinhas
        userPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // Desabilita a edição dos campos inicialmente
        userName.setEnabled(false);
        userEmail.setEnabled(false);
        userNumber.setEnabled(false);
        userPassword.setEnabled(false);

        // Recupera o objeto Usuario passado pela activity anterior
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Verifica se o objeto Usuario foi recebido corretamente
        if (usuario != null) {
            String nomeCompleto = usuario.getName() + " " + usuario.getSurname();
            userName.setText(nomeCompleto);
            userEmail.setText(usuario.getEmail());
            userNumber.setText(usuario.getNumber());
            userPassword.setText(usuario.getPassword());
            editProfileButton.setEnabled(true);
        }

        // Botão Editar/Salvar
        editProfileButton.setOnClickListener(v -> {
            boolean isEnabled = !userEmail.isEnabled();
            userEmail.setEnabled(isEnabled);
            userNumber.setEnabled(isEnabled);
            userPassword.setEnabled(isEnabled);
            editProfileButton.setText(isEnabled ? "Salvar" : "Editar conta");
            isEditing = isEnabled;

            if (!isEnabled) {
                usuario.setSurname(userEmail.getText().toString().trim());
                usuario.setNumber(userNumber.getText().toString().trim());
                usuario.setPassword(userPassword.getText().toString().trim());

                String nomeCompleto = usuario.getName() + " " + usuario.getSurname();
                userName.setText(nomeCompleto);
                Toast.makeText(editProfile.this, "Perfil atualizado", Toast.LENGTH_SHORT).show();
            }
        });

        // Botão Deletar
        deleteProfileButton.setOnClickListener(v -> {

        });

        // Botão Voltar
        backButton.setOnClickListener(v -> {
            if (isEditing) {
                usuario.setEmail(userEmail.getText().toString().trim());
                usuario.setNumber(userNumber.getText().toString().trim());
                usuario.setPassword(userPassword.getText().toString().trim());
            }

            Intent intent;
            if ("Motorista".equals(usuario.getTipoConta())) {
                intent = new Intent(editProfile.this, MenuRider.class);
            } else {
                intent = new Intent(editProfile.this, Menu.class);
            }

            intent.putExtra("usuario", usuario);
            startActivity(intent);
            finish();
        });
    }
}
