package br.fecap.pi.saferide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
// import android.util.Log; // Não usado diretamente após as modificações
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // Adicionado para feedback ao usuário

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Removidos imports do Volley e JSON, pois não faremos chamada de rede aqui
// import com.android.volley.Request;
// import com.android.volley.RequestQueue;
// import com.android.volley.toolbox.StringRequest;
// import com.android.volley.toolbox.Volley;
// import org.json.JSONException;
// import org.json.JSONObject;

// Removido import do CryptoUtils por enquanto, assumindo que nome/sobrenome não serão criptografados pelo cliente para o servidor
// import br.fecap.pi.saferide.security.CryptoUtils;

public class Name extends AppCompatActivity {

    private EditText editName, editSurname;
    private Button buttonNext, buttonBack;
    // Removido o campo 'usuario' aqui, pois ele será criado e passado no clique do botão
    // private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.name); // Certifique-se que R.layout.name é o seu layout correto

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editName = findViewById(R.id.editName);
        editSurname = findViewById(R.id.editSurname);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        buttonNext.setEnabled(false);

        editName.addTextChangedListener(textWatcher);
        editSurname.addTextChangedListener(textWatcher);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString().trim();
                String surname = editSurname.getText().toString().trim();

                // Validação simples (TextWatcher já faz a validação de não vazio)
                if (name.length() < 2) { // Exemplo de validação de tamanho mínimo
                    editName.setError("Nome muito curto");
                    editName.requestFocus();
                    return;
                }
                if (surname.length() < 2) {
                    editSurname.setError("Sobrenome muito curto");
                    editSurname.requestFocus();
                    return;
                }

                // Cria um NOVO objeto Usuario, já que esta é a primeira tela de cadastro.
                // Nas próximas Activities do fluxo, você recuperaria o objeto Usuario da Intent,
                // adicionaria os novos dados e o passaria adiante.
                // O construtor do Usuario foi adaptado para aceitar os campos que temos até agora.
                // Se seu construtor for diferente, ajuste aqui ou crie um construtor vazio e use setters.
                Usuario novoUsuario = new Usuario(name, surname, null, null); // Email e telefone serão preenchidos depois


                Intent intent = new Intent(Name.this, subclasse.class);
                intent.putExtra("usuario_parcial", novoUsuario); // Passa o objeto com os dados coletados
                startActivity(intent);
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Name.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String name = editName.getText().toString().trim();
            String surname = editSurname.getText().toString().trim();
            buttonNext.setEnabled(!name.isEmpty() && !surname.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };


}