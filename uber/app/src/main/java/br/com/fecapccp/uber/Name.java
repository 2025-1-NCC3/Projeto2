package br.com.fecapccp.uber;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Name extends AppCompatActivity {

    private EditText editName, editSurname;
    private Button buttonNext, buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        EdgeToEdge.enable(this);
        setContentView(R.layout.name);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa os componentes EditText e Button
        editName = findViewById(R.id.editName);
        editSurname = findViewById(R.id.editSurname);
        buttonNext = findViewById(R.id.next);
        buttonBack = findViewById(R.id.back);

        // Desabilita o botão "Próximo" inicialmente
        buttonNext.setEnabled(false);

        // Adiciona listeners para os campos de texto - servem para validar se os campos são preenchidos
        editName.addTextChangedListener(textWatcher);
        editSurname.addTextChangedListener(textWatcher);

        // Configura o botão "Próximo"
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString().trim(); //Faz a conversão do text para string
                String surname = editSurname.getText().toString().trim();
                Usuario usuario = new Usuario(name, surname, "", ""); //Objeto usuario da classe Usuario que obtém os valores de name e surname

                Intent intent = new Intent(Name.this, Email.class); //Navega de name para email ao clicar em próximo
                intent.putExtra("usuario", usuario); //Faz com que o usuário seja adicionado como um extra em Intent para que seja acessado por outra página por meio de Serializable
                startActivity(intent); //Inicia a activity
            }
        });

        // Configura o botão "Voltar"
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Volta para a tela de Login
                Intent intent = new Intent(Name.this, Login.class);
                startActivity(intent);
                finish(); // Finaliza a atividade atual para liberar memória
            }
        });
    }

    // TextWatcher para monitorar os campos de texto
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Verifica se ambos os campos estão preenchidos
            String name = editName.getText().toString().trim();
            String surname = editSurname.getText().toString().trim();

            // Habilita o botão "Próximo" somente se ambos os campos estiverem preenchidos
            buttonNext.setEnabled(!name.isEmpty() && !surname.isEmpty());
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };
}