package br.fecapccp.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class Login extends AppCompatActivity {

    private TextView text_tela_cadastro;
    private EditText editEmail, editSenha;
    private Button buttonLogin;

    // Lista estática para armazenar usuários cadastrados
    private static List<Usuario> usuariosCadastrados = new ArrayList<>();

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

        // Inicializa componentes
        text_tela_cadastro = findViewById(R.id.text_cadastro);
        editEmail = findViewById(R.id.editTextText1);  // Ajustado para o ID correto
        editSenha = findViewById(R.id.editTextText2);  // Ajustado para o ID correto
        buttonLogin = findViewById(R.id.button2);  // Ajustado para o ID correto

        // Listener para tela de cadastro
        text_tela_cadastro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, Name.class);
                startActivity(intent);
            }
        });

        // Listener para login
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();
                String senha = editSenha.getText().toString().trim();

                if (validarLogin(email, senha)) {
                    // Login bem-sucedido
                    Usuario usuarioLogado = getUsuarioPorEmail(email);

                    Intent intent = new Intent(Login.this, Menu.class);
                    intent.putExtra("usuario", usuarioLogado);
                    startActivity(intent);
                    finish();
                } else {
                    // Login falhou
                    Toast.makeText(Login.this, "Email ou senha inválidos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Método para cadastrar usuário
    public static void cadastrarUsuario(Usuario usuario) {
        // Verificar se já existe usuário com o mesmo email
        for (Usuario u : usuariosCadastrados) {
            if (u.getEmail().equals(usuario.getEmail())) {
                return; // Usuário já cadastrado
            }
        }
        usuariosCadastrados.add(usuario);
    }

    // Método para validar login
    private boolean validarLogin(String email, String senha) {
        for (Usuario usuario : usuariosCadastrados) {
            if (usuario.getEmail().equals(email)){
                String salt = usuario.getSalt();
                String hashedInput = PasswordUtils.hashPassword(senha, salt);
                return hashedInput.equals(usuario.getPassword());
            }
        }
        return false;
    }

    // Método para recuperar usuário pelo email
    private Usuario getUsuarioPorEmail(String email) {
        for (Usuario usuario : usuariosCadastrados) {
            if (usuario.getEmail().equals(email)) {
                return usuario;
            }
        }
        return null;
    }
}