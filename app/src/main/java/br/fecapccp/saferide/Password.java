package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Password extends AppCompatActivity {

    private EditText editPassword;
    private Button buttonNext, buttonBack;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.password);

        // Ajusta o padding para evitar sobreposição com as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade Number
        usuario = (Usuario) getIntent().getSerializableExtra("usuario");

        // Inicializa os componentes
        editPassword = findViewById(R.id.editPassword);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // Desabilita o botão "Próximo" inicialmente
        buttonNext.setEnabled(false);

        // Adiciona o TextWatcher ao campo de senha
        editPassword.addTextChangedListener(textWatcher);

        // Configura o botão "Próximo"
        buttonNext.setOnClickListener(v -> {
            String password = editPassword.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(Password.this, "Digite a senha!", Toast.LENGTH_SHORT).show();
            } else {
                // Atualiza o objeto Usuario com a senha
                usuario.setPassword(password);

                // IMPORTANTE: Adicione esta linha para cadastrar o usuário
                Login.cadastrarUsuario(usuario);

                // Passa o objeto Usuario para a próxima atividade (Menu)
                Intent intent = new Intent(Password.this, Menu.class);
                intent.putExtra("usuario", usuario);
                startActivity(intent);
            }
        });

        // Configura o botão "Voltar"
        buttonBack.setOnClickListener(v -> {
            // Volta para a tela de Number
            Intent intent = new Intent(Password.this, Number.class);
            startActivity(intent);
            finish(); // Finaliza a atividade atual
        });
    }

    // TextWatcher para validar o campo de senha
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Não é necessário implementar
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Habilita o botão "Próximo" se o campo de senha não estiver vazio
            String password = editPassword.getText().toString().trim();
            buttonNext.setEnabled(!password.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Não é necessário implementar
        }
    };
}