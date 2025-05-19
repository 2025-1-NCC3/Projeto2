package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // Mantido para logs, se necessário
import android.util.Patterns;
import android.view.View;
import android.widget.Button; // Usando android.widget.Button conforme seu código original
import android.widget.EditText;
import android.widget.Toast; // Para feedback ao usuário

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class Email extends AppCompatActivity {

    private static final String TAG = "EmailActivity"; // TAG específica
    private EditText editEmail;
    private Button buttonNext, buttonBack; // Se estiver usando MaterialButton no XML, declare como MaterialButton
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.email); // Certifique-se que R.layout.email é seu layout correto

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Recupera o objeto Usuario passado pela atividade anterior
        // Usando a chave consistente "usuario_parcial"
        if (getIntent().hasExtra("usuario_parcial")) {
            usuario = (Usuario) getIntent().getSerializableExtra("usuario_parcial");
        }

        if (usuario == null) {
            Toast.makeText(this, "Erro: Dados de cadastro incompletos. Reiniciando o fluxo.", Toast.LENGTH_LONG).show();
            // Redireciona para a primeira tela do cadastro para evitar erros
            Intent intent = new Intent(this, Name.class); // Ou qualquer que seja sua primeira Activity de cadastro
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finaliza esta activity
            return; // Interrompe a execução do onCreate
        }

        editEmail = findViewById(R.id.editEmail);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        buttonNext.setEnabled(false); // Botão "Próximo" começa desabilitado

        editEmail.addTextChangedListener(textWatcher);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString().trim();

                // A validação de formato já é feita pelo TextWatcher para habilitar o botão,
                // mas uma verificação final aqui não faz mal.
                if (!isValidEmail(email)) {
                    editEmail.setError("Formato de e-mail inválido.");
                    editEmail.requestFocus();
                    return;
                }

                // Define o email (em texto plano) no objeto Usuario
                usuario.setEmail(email);

                // NENHUMA CRIPTOGRAFIA OU CHAMADA AO SERVIDOR AQUI

                // Passa o objeto Usuario atualizado para a próxima atividade (Number.class)
                Intent intent = new Intent(Email.this, Number.class); // Próxima Activity
                intent.putExtra("usuario_parcial", usuario); // Continua usando a mesma chave
                startActivity(intent);
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volta para a tela de Genero
                finish(); // Apenas finaliza a activity atual para voltar para a anterior na pilha
            }
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String email = editEmail.getText().toString().trim();
            buttonNext.setEnabled(isValidEmail(email));
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private boolean isValidEmail(String email) {
        // Verifica se não está vazio E se corresponde ao padrão de email do Android
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

}