package br.fecapccp.saferide;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class editProfile extends AppCompatActivity {

    private EditText userName, userSurname, userEmail, userNumber;
    private Button editProfileButton, deleteProfileButton;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.edit_profile);

        // Ajusta o padding da view principal para evitar sobreposição com as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa os componentes
        userName = findViewById(R.id.userName);
        userSurname = findViewById(R.id.userSurname);
        userEmail = findViewById(R.id.userEmail);
        userNumber = findViewById(R.id.userNumber);
        editProfileButton = findViewById(R.id.editProfile);
        deleteProfileButton = findViewById(R.id.deleteProfile);

        // Desabilita a edição dos campos inicialmente
        userName.setEnabled(false);
        userSurname.setEnabled(false);
        userEmail.setEnabled(false);
        userNumber.setEnabled(false);

        // Recupera o objeto Usuario passado pela atividade anterior (Number)
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Verifica se o objeto Usuario foi recebido corretamente
        if (usuario != null) {
            userName.setText(usuario.getName());
            userSurname.setText(usuario.getSurname());
            userEmail.setText(usuario.getEmail());
            userNumber.setText(usuario.getNumber());

            // Habilita o botão "Editar perfil" após carregar os dados
            editProfileButton.setEnabled(true);
        }

        // Configura o listener do botão de editar perfil
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Habilita/desabilita a edição dos campos
                boolean isEnabled = !userName.isEnabled();
                userName.setEnabled(isEnabled);
                userSurname.setEnabled(isEnabled);
                userEmail.setEnabled(isEnabled);
                userNumber.setEnabled(isEnabled);

                // Altera o texto do botão
                editProfileButton.setText(isEnabled ? "Salvar" : "Editar");

                // Se estiver salvando, atualiza o objeto Usuario
                if (!isEnabled) {
                    usuario.setName(userName.getText().toString().trim());
                    usuario.setSurname(userSurname.getText().toString().trim());
                    usuario.setEmail(userEmail.getText().toString().trim());
                    usuario.setNumber(userNumber.getText().toString().trim());

                    // Implementar a lógica para editar os dados do usuário no Banco de Dados
                }
            }
        });

        // Configura o listener do botão de deletar perfil
        deleteProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implementar a lógica para apagar o Perfil no banco de dados
            }
        });
    }
}